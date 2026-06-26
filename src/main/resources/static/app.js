(function () {
    'use strict';

    // ── Upload page: drag-and-drop + image preview ──────────────────────

    var dropZone = document.getElementById('drop-zone');
    var fileInput = document.getElementById('file-input');
    var dropText = document.getElementById('drop-zone-text');
    var textarea = document.getElementById('description');
    var counter = document.getElementById('char-count');
    var form = document.getElementById('upload-form');
    var submitBtn = document.getElementById('submit-btn');

    function showPreview(file) {
        var reader = new FileReader();
        reader.onload = function (e) {
            var img = document.createElement('img');
            img.src = e.target.result;
            img.alt = escapeHtml(file.name);
            var hint = document.createElement('span');
            hint.className = 'change-hint';
            hint.textContent = 'Click to change';
            dropText.innerHTML = '';
            dropText.appendChild(img);
            dropText.appendChild(hint);
        };
        reader.readAsDataURL(file);
    }

    if (dropZone && fileInput && dropText) {
        dropZone.addEventListener('dragover', function (e) {
            e.preventDefault();
            dropZone.classList.add('dragover');
        });

        dropZone.addEventListener('dragleave', function () {
            dropZone.classList.remove('dragover');
        });

        dropZone.addEventListener('drop', function (e) {
            e.preventDefault();
            dropZone.classList.remove('dragover');
            var file = e.dataTransfer.files[0];
            if (file) {
                var dt = new DataTransfer();
                dt.items.add(file);
                fileInput.files = dt.files;
                showPreview(file);
            }
        });

        fileInput.addEventListener('change', function () {
            if (fileInput.files.length > 0) {
                showPreview(fileInput.files[0]);
            }
        });
    }

    // ── Upload + edit page: character counter ───────────────────────────

    if (textarea && counter) {
        function updateCounter() {
            counter.textContent = textarea.value.length + ' / 500';
        }
        textarea.addEventListener('input', updateCounter);
        updateCounter();
    }

    // ── Upload page: submit guard ────────────────────────────────────────

    if (form && submitBtn) {
        form.addEventListener('submit', function () {
            submitBtn.disabled = true;
            submitBtn.textContent = 'Uploading…';
        });
    }

    // ── Gallery page: fullscreen lightbox ────────────────────────────────

    var lightbox = document.getElementById('lightbox');
    var lightboxImg = document.getElementById('lightbox-img');
    var lightboxTitle = document.getElementById('lightbox-title');
    var lightboxArtist = document.getElementById('lightbox-artist');
    var lightboxClose = document.getElementById('lightbox-close');

    function openLightbox(img) {
        lightboxImg.src = img.dataset.src || img.src;
        lightboxImg.alt = img.alt;
        lightboxTitle.textContent = img.dataset.title || '';
        lightboxArtist.textContent = img.dataset.artist || '';
        lightbox.classList.add('visible');
        lightbox.removeAttribute('aria-hidden');
        document.body.style.overflow = 'hidden';
    }

    function closeLightbox() {
        lightbox.classList.remove('visible');
        lightbox.setAttribute('aria-hidden', 'true');
        lightboxImg.src = '';
        document.body.style.overflow = '';
    }

    if (lightbox) {
        document.querySelectorAll('.gallery-img').forEach(function (img) {
            img.style.cursor = 'zoom-in';
            img.addEventListener('click', function () {
                openLightbox(img);
            });
        });

        lightboxClose.addEventListener('click', closeLightbox);

        lightbox.addEventListener('click', function (e) {
            if (e.target === lightbox) closeLightbox();
        });

        document.addEventListener('keydown', function (e) {
            if (e.key === 'Escape' && lightbox.classList.contains('visible')) {
                closeLightbox();
            }
        });
    }

    // ── Gallery page: delete confirmation ────────────────────────────────

    document.querySelectorAll('.delete-form').forEach(function (f) {
        f.addEventListener('submit', function (e) {
            if (!confirm('Delete this artwork? This cannot be undone.')) {
                e.preventDefault();
            }
        });
    });

    // ── Utility ──────────────────────────────────────────────────────────

    function escapeHtml(str) {
        return str
            .replace(/&/g, '&amp;')
            .replace(/</g, '&lt;')
            .replace(/>/g, '&gt;')
            .replace(/"/g, '&quot;');
    }
}());
