const tg = window.Telegram.WebApp;
tg.ready();
tg.expand();

const user = tg.initDataUnsafe?.user;
const info = document.getElementById('user-info');

if (user) {
    info.innerHTML = `
        <p>Пользователь: <b>${user.username || '—'}</b></p>
        <p>Telegram ID: <b>${user.id}</b></p>
        <p>Имя: <b>${user.first_name}</b></p>
    `;
} else {
    info.innerHTML = '<p>Данные пользователя недоступны</p>';
}

// Отправка initData на backend
if (tg.initData) {
    fetch('/api/webapp/auth', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ initData: tg.initData })
    })
    .then(r => r.json())
    .then(data => console.log('Auth OK:', data))
    .catch(e => console.error('Auth error:', e));
}