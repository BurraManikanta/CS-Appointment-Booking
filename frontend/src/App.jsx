import { useState } from 'react';

async function readResponse(response) {
  const result = await response.json();
  if (!response.ok) {
    throw new Error(result.error || 'Request failed');
  }
  return result;
}

export default function App() {
  const [appointmentId, setAppointmentId] = useState('');
  const [message, setMessage] = useState('');
  const [busy, setBusy] = useState(false);

  async function book(event) {
    event.preventDefault();
    const form = new FormData(event.currentTarget);
    setBusy(true);
    setMessage('');
    try {
      const appointment = await readResponse(await fetch('/api/appointments', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({
          providerId: form.get('providerId'),
          start: form.get('start'),
          durationSeconds: Number(form.get('durationMinutes')) * 60
        })
      }));
      setAppointmentId(appointment.id);
      setMessage(`Booked ${appointment.id}: ${appointment.start} – ${appointment.end}`);
    } catch (error) {
      setMessage(error.message);
    } finally {
      setBusy(false);
    }
  }

  async function cancel(event) {
    event.preventDefault();
    setBusy(true);
    setMessage('');
    try {
      const result = await readResponse(await fetch(
        `/api/appointments/${encodeURIComponent(appointmentId)}`,
        { method: 'DELETE' }
      ));
      setMessage(`Cancellation result: ${result.result}`);
    } catch (error) {
      setMessage(error.message);
    } finally {
      setBusy(false);
    }
  }

  return (
    <main className="page">
      <header>
        <p className="eyebrow">SCHEDULING</p>
        <h1>Appointment booking</h1>
        <p className="intro">Book or cancel a provider appointment. Times must be UTC.</p>
      </header>

      <section className="card">
        <h2>Book an appointment</h2>
        <form onSubmit={book}>
          <label>
            Provider ID
            <input name="providerId" placeholder="provider-1" required />
          </label>
          <label>
            Start time (UTC)
            <input name="start" type="text" placeholder="2026-11-01T10:00:00Z" required />
          </label>
          <label>
            Duration (minutes)
            <input name="durationMinutes" type="number" min="1" defaultValue="60" required />
          </label>
          <button disabled={busy}>{busy ? 'Please wait…' : 'Book appointment'}</button>
        </form>
      </section>

      <section className="card">
        <h2>Cancel an appointment</h2>
        <form onSubmit={cancel}>
          <label>
            Appointment ID
            <input
              value={appointmentId}
              onChange={(event) => setAppointmentId(event.target.value)}
              placeholder="ID from a booking"
              required
            />
          </label>
          <button className="secondary" disabled={busy}>Cancel appointment</button>
        </form>
      </section>

      {message && <p className="message" role="status" aria-live="polite">{message}</p>}
    </main>
  );
}
