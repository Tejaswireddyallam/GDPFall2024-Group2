const cron = require("node-cron");
const emails = require('./src/emails/mails.js');
const pool = require('./src/configs/pool.js');

const sendAppointmentReminders = async () => {
    try {
        console.log("🔄 Checking for next day's appointments...");

        const tomorrow = new Date();
        tomorrow.setDate(tomorrow.getDate() + 1);
        const formattedDate = `${tomorrow.getDate()}/${tomorrow.getMonth() + 1}/${tomorrow.getFullYear()}`;

        const fetchQuery = `
            SELECT 
                a.AppointmentID,
                a.SlotDate AS appointmentDate,
                a.SlotTime AS appointmentTime,
                a.Status,
                p.Name AS patientName,
                p.Email AS patientEmail,
                d.Name AS doctorName,
                d.Email As doctorEmail
            FROM Appointments a
            JOIN Patients p ON a.PatientID = p.PatientID
            JOIN Doctors d ON a.DoctorID = d.DoctorID
            WHERE a.SlotDate = ? AND a.Status NOT IN ('cancelled', 'completed')
        `;

        const [appointments] = await pool.execute(fetchQuery, [formattedDate]);

        if (appointments.length === 0) {
            console.log("✅ No upcoming appointments for tomorrow.");
            return;
        }

        console.log(`📩 Sending ${appointments.length} appointment reminders...`);

        for (const appointment of appointments) {
            const text = `Hello ${appointment.patientName}, 
            
            This is a reminder about your upcoming appointment with Dr. ${appointment.doctorName} scheduled for:
            📅 Date: ${appointment.appointmentDate}
            ⏰ Time: ${appointment.appointmentTime}

            Please ensure you join the meeting on time. If you need to reschedule or cancel, please contact us in advance.

            Thank you!`;

            emails.sendAppointmentUpdate(appointment.patientEmail, appointment.Status, text);
            emails.sendAppointmentRequest(appointment.doctorEmail, appointment.Status, text);
            console.log(`📨 Reminder sent to ${appointment.patientEmail} & ${appointment.doctorEmail}`);
        }

        console.log("✅ All reminders sent successfully.");
    } catch (error) {
        console.error("❌ Error sending appointment reminders:", error.message);
    }
};


cron.schedule("0 0 * * *", async () => {
    console.log("⏰ Running scheduled appointment reminder check...");
    await sendAppointmentReminders();
});

module.exports = sendAppointmentReminders
