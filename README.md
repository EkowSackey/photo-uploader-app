# Photo Uploader — Application

A containerised Spring Boot photo gallery that lets users upload images with descriptions. Images are stored in Amazon S3 and served via CloudFront; metadata is persisted in RDS PostgreSQL. Deployed on Amazon ECS Fargate with automated blue/green deployments via CodePipeline and CodeDeploy.

**Infrastructure repo:** [EkowSackey/photo-uploader-infra](https://github.com/EkowSackey/photo-uploader-infra)

---

## Tech Stack

| Layer | Technology |
|---|---|
| Language | Java 21 |
| Framework | Spring Boot 3.3.2 |
| Build | Maven (multi-stage Docker build) |
| Templating | Thymeleaf |
| Database | Spring Data JPA + PostgreSQL |
| Storage | AWS SDK v2 for S3 |
| Observability | Spring Actuator (`/actuator/health`) |
| Container runtime | Amazon ECS Fargate |

---

## Repository Structure

```
.
├── src/
│   ├── main/
│   │   ├── java/com/example/fotos/
│   │   │   ├── config/          # AwsConfig, S3Properties
│   │   │   ├── controller/      # PhotoController, LocalFileController
│   │   │   ├── entity/          # Photo
│   │   │   ├── repository/      # PhotoRepository
│   │   │   └── service/         # PhotoService, S3StorageService, LocalStorageService
│   │   └── resources/
│   │       ├── application.yml  # App config (env-var driven)
│   │       ├── static/          # CSS
│   │       └── templates/       # Thymeleaf HTML templates
│   └── test/                    # Unit tests (Mockito)
├── .github/workflows/
│   └── build-and-push.yml       # GitHub Actions CI/CD workflow
├── Dockerfile                   # Multi-stage build (Maven → JRE 21)
├── docker-compose.yml           # Local development (PostgreSQL)
├── appspec.yaml                 # CodeDeploy blue/green appspec
└── taskdef.json                 # ECS task definition template (placeholders)
```

---

## Local Development

### Prerequisites
- Docker + Docker Compose
- Java 21 (for running outside Docker)

### Run with Docker Compose

```bash
# Start PostgreSQL
docker compose up -d db

# Run the app on the local profile (disk storage, no AWS needed)
./mvnw spring-boot:run -Dspring-boot.run.profiles=local
```

Open [http://localhost:8080](http://localhost:8080).

The `local` Spring profile uses `LocalStorageService` — images are saved to `uploads/` on disk and served directly. No AWS credentials required.

### Run Tests

```bash
./mvnw test
```

### Build the Docker Image Locally

```bash
docker build -t fotos:local .

docker run --rm \
  --network host \
  -e SPRING_PROFILES_ACTIVE=local \
  -e DB_HOST=localhost \
  -e DB_PORT=5432 \
  -e DB_NAME=photodb \
  -e DB_USER=postgres \
  -e DB_PASSWORD=postgres \
  fotos:local
```


## CI/CD Pipeline

Every push to `main` triggers the following automated flow:

```
git push → GitHub Actions
             ├─ OIDC → AWS STS (no long-lived credentials)
             ├─ Fill taskdef.json placeholders (CloudFormation exports via sed)
             ├─ Upload bundle.zip (appspec.yaml + taskdef.json) → S3
             └─ Build & push Docker image → ECR (prod/fotos:latest)
                          │
                          └─ EventBridge (ECR push event)
                                    │
                                    └─ CodePipeline
                                              ├─ Source: ECR image artifact
                                              ├─ Source: S3 deploy bundle
                                              └─ Deploy: CodeDeployToECS
                                                          └─ Blue/green swap on ECS
```

### GitHub Actions Secrets Required

| Secret | Value |
|---|---|
| `AWS_ROLE_ARN` | ARN of the OIDC IAM role (output from infra stack) |
| `AWS_REGION` | `eu-central-1` |

### Why OIDC?

GitHub Actions requests a short-lived JWT from GitHub's OIDC provider. AWS STS validates the JWT against the trust policy on the IAM role and returns temporary credentials (valid 1 hour). No `AWS_ACCESS_KEY_ID` or `AWS_SECRET_ACCESS_KEY` are stored anywhere.

---

## Deployment Architecture

ECS tasks run in **private subnets** with no public IP. All outbound traffic uses:
- **VPC endpoints** for ECR image pulls and CloudWatch log delivery (traffic stays on the AWS network)
- **NAT Gateway** for other outbound internet traffic
- **S3 Gateway endpoint** for photos bucket access

The ALB sits in public subnets and forwards port 80 (production) and port 8080 (test/green) to the ECS target groups. CodeDeploy shifts traffic between blue and green target groups during each deployment, with a 5-minute termination window for old tasks.
