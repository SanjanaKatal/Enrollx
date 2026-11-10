/**
 * EnrollX — Course Registration & Real-Time Seat Locking
 * Interactive Client-Side Engine for College Project Demonstration
 */

document.addEventListener('DOMContentLoaded', function () {
    initCountdownTimer();
    initSeatRefresher();
    initDemoCredentialsFill();
    initPasswordToggle();
    initInstantCourseFilter();
    initAlertAutoDismiss();
    initCopyButtons();
});

/**
 * 1. REAL-TIME SEAT RESERVATION COUNTDOWN TIMER
 * Showpiece for the DBMS Concurrency & TTL demonstration
 */
function initCountdownTimer() {
    const timerElement = document.getElementById('reservationCountdown');
    if (!timerElement) return;

    let secondsRemaining = parseInt(timerElement.getAttribute('data-seconds-remaining'), 10);
    const reservationId = timerElement.getAttribute('data-reservation-id');
    const confirmBtn = document.getElementById('confirmEnrollmentBtn');
    const expiryBanner = document.getElementById('expiryAlertBanner');
    const progressFill = document.getElementById('countdownProgressBar');

    const totalDurationSeconds = 300; // 5 minutes TTL

    function updateDisplay() {
        if (secondsRemaining <= 0) {
            timerElement.textContent = "00:00";
            timerElement.className = 'countdown-digits critical';

            if (confirmBtn) {
                confirmBtn.disabled = true;
                confirmBtn.classList.add('disabled');
                confirmBtn.innerHTML = '<i class="bi bi-x-octagon-fill me-2"></i>Hold Expired';
            }
            if (expiryBanner) {
                expiryBanner.classList.remove('d-none');
            }
            if (progressFill) {
                progressFill.style.width = '0%';
                progressFill.className = 'progress-bar bg-danger';
            }
            return;
        }

        const minutes = Math.floor(secondsRemaining / 60);
        const seconds = secondsRemaining % 60;
        timerElement.textContent = String(minutes).padStart(2, '0') + ':' + String(seconds).padStart(2, '0');

        // Dynamic State Color Shifting
        timerElement.classList.remove('safe', 'warning', 'critical');
        if (progressFill) {
            const pct = Math.min(100, (secondsRemaining / totalDurationSeconds) * 100);
            progressFill.style.width = pct + '%';
        }

        if (secondsRemaining > 120) {
            timerElement.classList.add('safe');
            if (progressFill) progressFill.className = 'progress-bar bg-success';
        } else if (secondsRemaining > 60) {
            timerElement.classList.add('warning');
            if (progressFill) progressFill.className = 'progress-bar bg-warning';
        } else {
            timerElement.classList.add('critical');
            if (progressFill) progressFill.className = 'progress-bar bg-danger';
        }
    }

    // Initial render
    updateDisplay();

    // 1-second local tick
    const localTimer = setInterval(function () {
        if (secondsRemaining <= 0) {
            clearInterval(localTimer);
            clearInterval(syncTimer);
            return;
        }
        secondsRemaining--;
        updateDisplay();
    }, 1000);

    // Sync with server every 8 seconds to eliminate local clock drift
    const syncTimer = setInterval(function () {
        if (secondsRemaining <= 0) return;

        fetch('/api/reservations/' + reservationId + '/status')
            .then(res => {
                if (!res.ok) throw new Error('Network error');
                return res.json();
            })
            .then(data => {
                if (data.expired || data.secondsRemaining <= 0) {
                    secondsRemaining = 0;
                    updateDisplay();
                    clearInterval(localTimer);
                    clearInterval(syncTimer);
                } else {
                    secondsRemaining = data.secondsRemaining;
                    updateDisplay();
                }
            })
            .catch(err => console.debug('Sync skipped:', err));
    }, 8000);
}

/**
 * 2. LIVE DYNAMIC SEAT AVAILABILITY REFRESHER
 * Polls backend REST endpoint so students observe concurrent holds live
 */
function initSeatRefresher() {
    const seatCards = document.querySelectorAll('[data-offering-id]');
    if (seatCards.length === 0) return;

    setInterval(function () {
        seatCards.forEach(card => {
            const offeringId = card.getAttribute('data-offering-id');
            if (!offeringId) return;

            fetch('/api/offerings/' + offeringId + '/seat-status')
                .then(res => res.json())
                .then(data => {
                    const availableBadge = card.querySelector('.live-available-seats');
                    const reservedBadge = card.querySelector('.live-reserved-seats');
                    const enrolledBadge = card.querySelector('.live-enrolled-seats');
                    const progressBar = card.querySelector('.seat-progress-bar');
                    const reserveBtn = card.querySelector('.btn-reserve-seat');

                    if (availableBadge) {
                        availableBadge.textContent = data.availableSeats + ' Available';
                        availableBadge.className = 'seat-badge live-available-seats ' + 
                            (data.availableSeats > 0 ? 'seat-available' : 'seat-full');
                    }
                    if (reservedBadge) reservedBadge.textContent = data.reservedCount;
                    if (enrolledBadge) enrolledBadge.textContent = data.enrolledCount;

                    if (progressBar && data.capacity > 0) {
                        const filled = data.enrolledCount + data.reservedCount;
                        const pct = Math.min(100, Math.round((filled / data.capacity) * 100));
                        progressBar.style.width = pct + '%';
                        progressBar.className = 'progress-bar seat-progress-bar ' + 
                            (data.availableSeats <= 0 ? 'bg-danger' : (data.reservedCount > 0 ? 'bg-warning' : 'bg-primary'));
                    }

                    if (reserveBtn && !reserveBtn.classList.contains('already-enrolled')) {
                        if (data.availableSeats <= 0) {
                            reserveBtn.disabled = true;
                            reserveBtn.classList.add('disabled', 'btn-secondary');
                            reserveBtn.classList.remove('btn-primary-enrollx');
                            reserveBtn.innerHTML = '<i class="bi bi-slash-circle me-1"></i>Course Full';
                        }
                    }
                })
                .catch(e => console.debug('Polling skipped', e));
        });
    }, 10000);
}

/**
 * 3. QUICK DEMO FILL BUTTONS (For Viva Presentations)
 */
function initDemoCredentialsFill() {
    const fillAdminBtn = document.getElementById('demoFillAdmin');
    const fillStudent1Btn = document.getElementById('demoFillStudent1');
    const fillStudent2Btn = document.getElementById('demoFillStudent2');
    const usernameInput = document.getElementById('username');
    const passwordInput = document.getElementById('password');

    if (!usernameInput || !passwordInput) return;

    if (fillAdminBtn) {
        fillAdminBtn.addEventListener('click', function () {
            usernameInput.value = 'admin';
            passwordInput.value = 'Admin@123';
            highlightInputs(usernameInput, passwordInput);
        });
    }

    if (fillStudent1Btn) {
        fillStudent1Btn.addEventListener('click', function () {
            usernameInput.value = 'student1';
            passwordInput.value = 'Student@123';
            highlightInputs(usernameInput, passwordInput);
        });
    }

    if (fillStudent2Btn) {
        fillStudent2Btn.addEventListener('click', function () {
            usernameInput.value = 'student2';
            passwordInput.value = 'Student@123';
            highlightInputs(usernameInput, passwordInput);
        });
    }

    function highlightInputs(u, p) {
        u.classList.add('border-primary');
        p.classList.add('border-primary');
        setTimeout(() => {
            u.classList.remove('border-primary');
            p.classList.remove('border-primary');
        }, 800);
    }
}

/**
 * 4. PASSWORD VISIBILITY TOGGLE
 */
function initPasswordToggle() {
    const toggleBtns = document.querySelectorAll('.toggle-password-btn');
    toggleBtns.forEach(btn => {
        btn.addEventListener('click', function () {
            const targetId = this.getAttribute('data-target');
            const input = document.getElementById(targetId);
            if (!input) return;

            const icon = this.querySelector('i');
            if (input.type === 'password') {
                input.type = 'text';
                if (icon) {
                    icon.classList.remove('bi-eye');
                    icon.classList.add('bi-eye-slash');
                }
            } else {
                input.type = 'password';
                if (icon) {
                    icon.classList.remove('bi-eye-slash');
                    icon.classList.add('bi-eye');
                }
            }
        });
    });
}

/**
 * 5. INSTANT CLIENT-SIDE COURSE FILTERING
 * Allows instant search-as-you-type and department filtering without slow reloads
 */
function initInstantCourseFilter() {
    const searchInput = document.getElementById('instantCourseSearch');
    const deptPills = document.querySelectorAll('.dept-filter-pill');
    const cards = document.querySelectorAll('.course-offering-card');

    if (!cards.length) return;

    function applyFilter() {
        const query = searchInput ? searchInput.value.toLowerCase().trim() : '';
        const activeDeptPill = document.querySelector('.dept-filter-pill.active');
        const selectedDept = activeDeptPill ? activeDeptPill.getAttribute('data-dept-code') : 'ALL';

        let visibleCount = 0;

        cards.forEach(card => {
            const code = (card.getAttribute('data-course-code') || '').toLowerCase();
            const title = (card.getAttribute('data-course-title') || '').toLowerCase();
            const dept = card.getAttribute('data-dept-code') || '';

            const matchesQuery = !query || code.includes(query) || title.includes(query);
            const matchesDept = selectedDept === 'ALL' || dept === selectedDept;

            if (matchesQuery && matchesDept) {
                card.style.display = '';
                visibleCount++;
            } else {
                card.style.display = 'none';
            }
        });

        const emptyMessage = document.getElementById('clientEmptyCourses');
        if (emptyMessage) {
            emptyMessage.classList.toggle('d-none', visibleCount > 0);
        }
    }

    if (searchInput) {
        searchInput.addEventListener('input', applyFilter);
    }

    deptPills.forEach(pill => {
        pill.addEventListener('click', function (e) {
            e.preventDefault();
            deptPills.forEach(p => p.classList.remove('active'));
            this.classList.add('active');
            applyFilter();
        });
    });
}

/**
 * 6. AUTO-DISMISS BOOTSTRAP ALERTS
 */
function initAlertAutoDismiss() {
    setTimeout(function () {
        const alerts = document.querySelectorAll('.alert-auto-dismiss');
        alerts.forEach(alert => {
            try {
                const bsAlert = new bootstrap.Alert(alert);
                bsAlert.close();
            } catch (e) {
                // Ignore if bootstrap is not defined
            }
        });
    }, 7000);
}

/**
 * 7. CLIPBOARD COPY BUTTONS
 */
function initCopyButtons() {
    const copyBtns = document.querySelectorAll('.btn-copy-text');
    copyBtns.forEach(btn => {
        btn.addEventListener('click', function () {
            const textToCopy = this.getAttribute('data-copy');
            if (!textToCopy) return;

            navigator.clipboard.writeText(textToCopy).then(() => {
                const originalHtml = this.innerHTML;
                this.innerHTML = '<i class="bi bi-check2 text-success me-1"></i>Copied!';
                setTimeout(() => {
                    this.innerHTML = originalHtml;
                }, 2000);
            });
        });
    });
}
