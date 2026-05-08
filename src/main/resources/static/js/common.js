function getCsrfToken() {
    const tokenMeta = document.querySelector('meta[name="_csrf"]');
    const headerMeta = document.querySelector('meta[name="_csrf_header"]');
    if (!tokenMeta || !headerMeta) {
        return null;
    }
    return {
        token: tokenMeta.getAttribute('content'),
        header: headerMeta.getAttribute('content')
    };
}

const pendingApprovals = new Set();

function initSingleSubmitForms() {
    document.querySelectorAll('form[data-single-submit]').forEach((form) => {
        form.addEventListener('submit', (event) => {
            if (form.dataset.submitting === 'true') {
                event.preventDefault();
                return;
            }

            const submitter = event.submitter;
            window.setTimeout(() => {
                if (event.defaultPrevented || event.returnValue === false || form.dataset.submitting === 'true') {
                    return;
                }

                form.dataset.submitting = 'true';
                form.querySelectorAll('button[type="submit"], input[type="submit"]').forEach((element) => {
                    element.disabled = true;
                });

                if (submitter instanceof HTMLButtonElement) {
                    submitter.textContent = submitter.dataset.loadingText || '처리 중...';
                } else if (submitter instanceof HTMLInputElement) {
                    submitter.value = submitter.dataset.loadingText || '처리 중...';
                }
            }, 0);
        });
    });
}

async function approveItem(itemId) {
    if (pendingApprovals.has(itemId)) {
        return;
    }

    pendingApprovals.add(itemId);
    const csrf = getCsrfToken();
    const headers = {};
    if (csrf) {
        headers[csrf.header] = csrf.token;
    }

    const approveButton = document.querySelector(`[data-approve-id="${itemId}"]`);
    if (approveButton) {
        approveButton.disabled = true;
        approveButton.textContent = '처리 중...';
    }

    try {
        const response = await fetch(`/admin/approve-ajax/${itemId}`, {
            method: 'POST',
            headers
        });

        if (!response.ok) {
            throw new Error('승인 실패');
        }

        const element = document.getElementById(`pending-item-${itemId}`);
        if (element) {
            element.classList.add('is-removing');
            setTimeout(() => element.remove(), 180);
        }

        setTimeout(() => {
            if (document.querySelectorAll('.pending-item').length === 0) {
                const emptyMessage = document.getElementById('empty-message');
                if (emptyMessage) {
                    emptyMessage.style.display = 'block';
                }
            }
        }, 220);
    } catch (error) {
        pendingApprovals.delete(itemId);
        if (approveButton) {
            approveButton.disabled = false;
            approveButton.textContent = '승인';
        }
        alert('승인 처리 중 오류가 발생했습니다. 다시 시도해 주세요.');
    }
}

document.addEventListener('DOMContentLoaded', initSingleSubmitForms);
