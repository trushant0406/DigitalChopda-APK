self.addEventListener('push', (event) => {
  const data = event.data ? event.data.json() : {};
  event.waitUntil(
    self.registration.showNotification(data.title || 'Call Intelligence', {
      body: data.body || '',
      icon: '/vite.svg',
    })
  );
});
