const jwt = require("jsonwebtoken");
const bcrypt = require('bcrypt');
const pool = require("../configs/pool.js");
const path = require('path');
const dotenv = require('dotenv').config()
const crypto = require('crypto');
const emails = require('../emails/mails.js')
var savedOTPS = {
};

const signup = async (req, res) => {
    try {
        console.log("Patient Signup -> ", req.body);
        const { fullname, email, password, contact, role, address } = req.body;

        if (!fullname || !email || !password) {
            return res.status(400).json({ message: 'Please provide all required fields.' });
        }

        const [existingUser] = await pool.execute('SELECT * FROM Patients WHERE Email = ?', [email]);
        if (existingUser.length > 0) {
            return res.status(409).json({ message: 'Email is already registered.' });
        }

        const hashedPassword = await bcrypt.hash(password, 10);
        const insertQuery = 'INSERT INTO Patients (Name, Email, Password, Role, ContactInfo, Address, IsVerified) VALUES (?, ?, ?, ?, ?, ?, ?)';
        const [result] = await pool.execute(insertQuery, [fullname, email, hashedPassword, role, contact, address, false]);

        if (result.affectedRows > 0) {
 
            const token = jwt.sign({ email }, process.env.JWT_TOKEN_KEY, { expiresIn: '1h' });

            const verificationUrl = `http://localhost:${process.env.PORT}/api/patient/verify-email?token=${token}`;
            emails.sendVerificationEmail(email, verificationUrl);

            return res.status(201).json({ message: 'Registration successful. Please verify your email.' });
        } else {
            return res.status(500).json({ message: 'Registration Failed.' });
        }
    } catch (error) {
        console.error('Error during signup:', error.message);
        res.status(500).json({ message: 'An error occurred during Registration.' });
    }
};

const login = async (req, res) => {
    try {
        console.log("Patient Login -> ", req.body);
        const { email, password } = req.body;
        if (!email || !password) {
            return res.status(400).json({ message: 'Please provide both email and password.' });
        }

        const [user] = await pool.execute('SELECT * FROM Patients WHERE Email = ?', [email]);
        console.log(user)
        if (user.length === 0) {
            return res.status(200).json({ success: false, message: 'Invalid email or password.' });
        }

        const foundUser = user[0];
        const isPasswordValid = await bcrypt.compare(password, foundUser.Password);
        if (!isPasswordValid) {
            return res.status(401).json({success: false, message: 'Invalid email or password.' });
        }

        const tokenPayload = { id: foundUser.PatientID, name: foundUser.Name };
        const token = jwt.sign(tokenPayload, process.env.JWT_TOKEN_KEY, { expiresIn: '7d' });

        const userResponse = {
            id: foundUser.PatientID,
            name: foundUser.Name,
            email: foundUser.Email,
            role: foundUser.Role,
            contact: foundUser.ContactInfo,
            isVerified: foundUser.isVerified,
            Token: token
        };

        return res.status(200).json({
        success: true,
        message: 'Login successful.',
        data: { user: userResponse },
        });

    } catch (error) {
        console.error('Error during login:', error.message);
        res.status(500).json({success: false, message: 'An error occurred during login.' });
    }
};

const forgotPassword = async (req, res) => {
    try {
        console.log("Forgot Password -> ", req.body);
        const { email } = req.body;

        if (!email) {
            return res.status(400).json({ success: false, message: "Please provide an email." });
        }

        const [user] = await pool.execute("SELECT * FROM Patients WHERE Email = ?", [email]);
        if (user.length === 0) {
            return res.status(404).json({ success: false, message: "Email not found." });
        }

        const resetToken = crypto.randomBytes(32).toString("hex");
        const hashedToken = await bcrypt.hash(resetToken, 10);
        const expiresAt = new Date(Date.now() + 30 * 60 * 1000);


        await pool.execute(
            "UPDATE Patients SET ResetToken = ?, ResetTokenExpires = ? WHERE Email = ?",
            [hashedToken, expiresAt, email]
        );

        const resetUrl = `${process.env.IP}/api/patient/reset-password?token=${resetToken}&email=${email}`;

        await emails.sendPasswordResetLink(email, resetUrl);

        return res.status(200).json({ success: true, message: "Password reset link sent to your email." });

    } catch (error) {
        console.error("Error in forgot password:", error.message);
        return res.status(500).json({ success: false, message: "An error occurred." });
    }
};

const resetPassword = async (req, res) => {
    try {
        console.log("Reset Password -> ", req.body);
        const { token, email, newPassword } = req.body;

        if (!token || !email || !newPassword) {
            return res.status(400).json({ success: false, message: "Invalid request." });
        }

        const [user] = await pool.execute("SELECT * FROM Patients WHERE Email = ?", [email]);
        if (user.length === 0) {
            return res.status(404).json({ success: false, message: "Invalid token or user not found." });
        }

        const foundUser = user[0];

        if (!foundUser.ResetTokenExpires || new Date(foundUser.ResetTokenExpires) < new Date()) {
            return res.status(400).json({ success: false, message: "Token expired. Request a new one." });
        }

        const isValidToken = await bcrypt.compare(token, foundUser.ResetToken);
        if (!isValidToken) {
            return res.status(400).json({ success: false, message: "Invalid or expired token." });
        }

        const hashedPassword = await bcrypt.hash(newPassword, 10);
        await pool.execute(
            "UPDATE Patients SET Password = ?, ResetToken = NULL, ResetTokenExpires = NULL WHERE Email = ?",
            [hashedPassword, email]
        );

        return res.status(200).json({ success: true, message: "Password reset successfully." });

    } catch (error) {
        console.error("Error in reset password:", error.message);
        return res.status(500).json({ success: false, message: "An error occurred." });
    }
};


const fetchProfile = async(req,res) => {

    try {
        console.log("Fetch profile request")
        console.log("Patient Profile -> ", req.body);
        const { email } = req.body;
        if (!email) {
            return res.status(400).json({ message: 'Please provide both email and password.' });
        }

        const [user] = await pool.execute('SELECT * FROM Patients WHERE Email = ?', [email]);
        console.log(user)
        if (user.length === 0) {
            return res.status(200).json({ success: false, message: 'Invalid email' });
        }

        const foundUser = user[0];
        const userResponse = {
            name: foundUser.Name,
            email: foundUser.Email,
            contact: foundUser.ContactInfo,
            address: foundUser.Address,
            gender: foundUser.Gender,
            dob: foundUser.DOB
        };

        return res.status(200).json({
        success: true,
        message: 'Login successful.',
        data: { user: userResponse },
        });

    } catch (error) {
        console.error('Error during login:', error.message);
        res.status(500).json({success: false, message: 'An error occurred during login.' });
    }

}

const validate = async (req, res) => {
    try {
        console.log("Token Validation -> ", req.body);
        const { email, token } = req.body;

        if (!email || !token) {
            return res.status(400).json({ message: 'Please provide both email and token.' });
        }

        const [user] = await pool.execute('SELECT * FROM Patients WHERE Email = ?', [email]);
        if (user.length === 0) {
            return res.status(404).json({ success: false, message: 'User not found.' });
        }

        const foundUser = user[0];

        try {
            const decodedToken = jwt.verify(token, process.env.JWT_TOKEN_KEY);

            if (decodedToken.id !== foundUser.PatientID || decodedToken.name !== foundUser.Name) {
                return res.status(401).json({ success: false, message: 'Invalid token.' });
            }

            const userResponse = {
                id: foundUser.PatientID,
                name: foundUser.Name,
                email: foundUser.Email,
                role: foundUser.Role,
                contact: foundUser.ContactInfo,
                isVerified: foundUser.isVerified,
                Token: token
            };

            return res.status(200).json({
                success: true,
                message: 'Token is valid.',
                data: { user: userResponse },
            });

        } catch (err) {
            console.error('Token verification error:', err.message);
            return res.status(401).json({ success: false, message: 'Invalid or expired token.' });
        }
    } catch (error) {
        console.error('Error during token validation:', error.message);
        res.status(500).json({ success: false, message: 'An error occurred during token validation.' });
    }
};

const verifyEmail = async (req, res) => {
    try {
        const { token } = req.query;

        if (!token) {
            return res.status(400).json({ message: 'Invalid or missing token.' });
        }

        const decoded = jwt.verify(token, process.env.JWT_TOKEN_KEY);
        
        const [result] = await pool.execute('UPDATE Patients SET IsVerified = ? WHERE Email = ?', [true, decoded.email]);

        if (result.affectedRows > 0) {
            return res.status(200).send('Email verified successfully!');
        } else {
            return res.status(400).send('Invalid token or user not found.');
        }
    } catch (error) {
        console.error('Error verifying email:', error.message);
        res.status(500).json({ message: 'An error occurred during email verification.' });
    }
};

const updateProfile = async (req, res) => {
    try {
        const { fullname, email, contact, address, gender, dob } = req.body;

        if (!email) {
            return res.status(400).json({ message: 'Email is required.' });
        }

        const updateQuery = `
            UPDATE Patients 
            SET Name = ?, ContactInfo = ?, Address = ?, Gender = ?, DOB = ?
            WHERE Email = ?
        `;

        const [result] = await pool.execute(updateQuery, [fullname, contact, address, gender, dob, email]);

        if (result.affectedRows > 0) {
            return res.status(200).json({success: true, message: 'Profile updated successfully.' });
        } else {
            return res.status(404).json({success: false, message: 'No changes made or user not found.' });
        }
    } catch (error) {
        console.error('Error updating profile:', error.message);
        return res.status(500).json({ message: 'An error occurred while updating the profile.' });
    }
};

const scheduleAppointment = async (req, res) => {
    try {
        console.log("Scheduling Appointment -> ", req.body);
        const { patientId, doctorId, adminId, date, time, status } = req.body;

        if (!patientId || !doctorId || !date || !time) {
            return res.status(400).json({ success: false, message: 'Please provide all required fields.' });
        }

        const insertQuery = `
            INSERT INTO Appointments (PatientID, DoctorID, AdminID, SlotDate, SlotTime, Status) 
            VALUES (?, ?, ?, ?, ?, ?)`;
        const [result] = await pool.execute(insertQuery, [
            patientId,
            doctorId,
            adminId || "123",
            date,
            time,
            status || "Pending", 
        ]);

        if (result.affectedRows > 0) {

            const fetchQuery = `
                SELECT 
                    a.AppointmentID,
                    a.SlotDate AS appointmentDate,
                    a.SlotTime AS appointmentTime,
                    a.Status,
                    p.Name AS patientName,
                    d.Name AS doctorName,
                    d.Email AS doctorEmail
                FROM Appointments a
                JOIN Patients p ON a.PatientID = p.PatientID
                JOIN Doctors d ON a.DoctorID = d.DoctorID
                WHERE a.AppointmentID = ?`;

            const [appointmentDetails] = await pool.execute(fetchQuery, [result.insertId]);

            if (appointmentDetails.length === 0) {
                return res.status(404).json({
                    success: false,
                    message: "Failed to retrieve appointment details after scheduling.",
                });
            }

            const scheduledAppointment = appointmentDetails[0];

            let text = `Dear Dr.${scheduledAppointment.doctorName}, you have a new appointment request from ${scheduledAppointment.patientName} scheduled for Date - ${scheduledAppointment.appointmentDate} & Time : ${scheduledAppointment.appointmentTime}. Please review and confirm. Thank You.`;

            emails.sendAppointmentRequest(scheduledAppointment.doctorEmail, status || "Pending", text);

            return res.status(201).json({
                success: true,
                message: "Appointment scheduled successfully.",
            });
        } else {
            return res.status(500).json({
                success: false,
                message: "Failed to schedule appointment.",
            });
        }
    } catch (error) {
        console.error("Error scheduling appointment:", error.message);
        res.status(500).json({
            success: false,
            message: "An error occurred while scheduling the appointment.",
        });
    }
};

const getAppointmentsByPatient = async (req, res) => {
    try {
        const { patientID } = req.params;

        if (!patientID) {
            return res.status(400).json({success: false, message: 'Patient ID is required.' });
        }

        const selectQuery = `
            SELECT Appointments.*, Doctors.Name AS DoctorName 
            FROM Appointments 
            JOIN Doctors ON Appointments.DoctorID = Doctors.DoctorID 
            WHERE Appointments.PatientID = ? 
            ORDER BY Appointments.SlotDate ASC;
        `;
        const [appointments] = await pool.execute(selectQuery, [patientID]);

        if (appointments.length > 0) {
            return res.status(200).json({ success: true, appointments });
        } else {
            return res.status(404).json({ success: false, message: 'No appointments found for this patient.' });
        }
    } catch (error) {
        console.error('Error retrieving appointments:', error.message);
        res.status(500).json({success: false, message: 'An error occurred while retrieving appointments.' });
    }
};

const updateAppointmentStatus = async (req, res) => {
    try {
        console.log("Updating Appointment -> ", req.body);
        const { appointmentID, status } = req.body;

        if (!appointmentID || !status) {
            return res.status(400).json({success: false, message: 'Please provide both appointment ID and status.' });
        }

        const updateQuery = `UPDATE Appointments SET Status = ? WHERE AppointmentID = ?`;
        const [result] = await pool.execute(updateQuery, [status, appointmentID]);

        if (result.affectedRows > 0) {

            if(status == 'cancelled'){

                const fetchQuery = `
                    SELECT 
                        a.AppointmentID, 
                        a.SlotDate AS appointmentDate,
                        a.SlotTime AS appointmentTime,
                        p.PatientID,
                        p.Name AS patientName,
                        d.DoctorID,
                        d.Name AS doctorName,
                        d.Email AS doctorEmail
                    FROM Appointments a
                    JOIN Patients p ON a.PatientID = p.PatientID
                    JOIN Doctors d ON a.DoctorID = d.DoctorID
                    WHERE a.AppointmentID = ?`;

                const [appointmentData] = await pool.execute(fetchQuery, [appointmentID]);

                let sub = 'Cancelled';
                let text = `Hello ${appointmentData.doctorName}, Your Appointment with ID : ${appointmentID} scheduled on ${appointmentData.appointmentDate} at ${appointmentData.appointmentTime} with ${appointmentData.patientName} was cancelled as requested from patient side.`

                emails.sendAppointmentUpdate(appointmentData.doctorEmail, sub, text);
            }
            return res.status(200).json({ success: true, message: 'Appointment status updated successfully.' });
        } else {
            return res.status(404).json({ success: false, message: 'Appointment not found.' });
        }
    } catch (error) {
        console.error('Error updating appointment status:', error.message);
        res.status(500).json({ success: false, message: 'An error occurred while updating appointment status.' });
    }
};

const rescheduleAppointment = async (req, res) => {
    try {
        console.log("Patient Reschedule Appointment -> ", req.body);
        const { appointmentID, SlotDate, SlotTime, status } = req.body;

        if (!appointmentID || !SlotDate || !SlotTime || !status) {
            return res.status(400).json({
                success: false,
                message: "Please provide appointment ID, SlotDate, SlotTime, and status.",
            });
        }

        const updateQuery = `
            UPDATE Appointments 
            SET SlotDate = ?, SlotTime = ?, Status = ? 
            WHERE AppointmentID = ?`;
        const [updateResult] = await pool.execute(updateQuery, [SlotDate, SlotTime, status, appointmentID]);

        if (updateResult.affectedRows > 0) {
            
            const fetchQuery = `
                SELECT 
                    a.AppointmentID,
                    a.SlotDate AS appointmentDate,
                    a.SlotTime AS appointmentTime,
                    a.Status,
                    p.Name AS patientName,
                    d.Name AS doctorName,
                    d.Email AS doctorEmail
                FROM Appointments a
                JOIN Patients p ON a.PatientID = p.PatientID
                JOIN Doctors d ON a.DoctorID = d.DoctorID
                WHERE a.AppointmentID = ?`;

            const [appointmentDetails] = await pool.execute(fetchQuery, [appointmentID]);

            if (appointmentDetails.length === 0) {
                return res.status(404).json({
                    success: false,
                    message: "Failed to retrieve updated appointment details.",
                });
            }

            const updatedAppointment = appointmentDetails[0];

            let text = `Dear Dr.${updatedAppointment.doctorName}, your appointment with ${updatedAppointment.patientName} has been rescheduled to Date - ${updatedAppointment.appointmentDate} & Time - ${updatedAppointment.appointmentTime}. Please review your schedule. Thank you.`;

            emails.sendRescheduleAppointment(updatedAppointment.doctorEmail, status, text);

            return res.status(200).json({
                success: true,
                message: "Appointment rescheduled successfully.",
            });
        } else {
            return res.status(404).json({
                success: false,
                message: "Appointment not found or no changes were made.",
            });
        }
    } catch (error) {
        console.error("Error rescheduling appointment:", error.message);
        res.status(500).json({
            success: false,
            message: "An error occurred while rescheduling the appointment.",
        });
    }
};

const deleteAppointment = async (req, res) => {
    try {
        const { appointmentID } = req.params;

        if (!appointmentID) {
            return res.status(400).json({ message: 'Appointment ID is required.' });
        }

        const deleteQuery = `DELETE FROM Appointments WHERE AppointmentID = ?`;
        const [result] = await pool.execute(deleteQuery, [appointmentID]);

        if (result.affectedRows > 0) {
            return res.status(200).json({success: true, message: 'Appointment deleted successfully.' });
        } else {
            return res.status(404).json({success: false, message: 'Appointment not found.' });
        }
    } catch (error) {
        console.error('Error deleting appointment:', error.message);
        res.status(500).json({success: false, message: 'An error occurred while deleting the appointment.' });
    }
};

const searchDoctors = async (req, res) => {
    try {
        const { name, specialization } = req.query;

        if (!name || !specialization) {
            return res.status(400).json({ message: "Please provide a name or specialization to search." });
        }

        let query = `SELECT DoctorID, Name, Specialization, ContactInfo, Email FROM Doctors WHERE 1=1`;
        let queryParams = [];

        if (name) {
            query += ` AND Name LIKE ?`;
            queryParams.push(`%${name}%`);
        }

        if (specialization) {
            query += ` AND Specialization LIKE ?`;
            queryParams.push(`%${specialization}%`);
        }

        const [doctors] = await pool.execute(query, queryParams);

        if (doctors.length > 0) {
            return res.status(200).json({success: true, doctors });
        } else {
            return res.status(404).json({success: false, message: "No doctors found matching the search criteria." });
        }
    } catch (error) {
        console.error("Error searching for doctors:", error.message);
        res.status(500).json({success: false, message: "An error occurred while searching for doctors." });
    }
};

const getDoctor = async (req, res) => {
    try {
        const { doctorId } = req.params;
        console.log(doctorId)
        if (!doctorId) {
            return res.status(400).json({success: false, message: "Please provide doctor id to search." });
        }

        let query = `SELECT Name FROM Doctors WHERE DoctorID = ?`;
   
        const [doctors] = await pool.execute(query, [doctorId]);

        if (doctors.length > 0) {
            return res.status(200).json({success: true, data: doctors });
        } else {
            return res.status(404).json({success: false, message: "No doctors found matching the search criteria." });
        }
    } catch (error) {
        console.error("Error searching for doctors:", error.message);
        res.status(500).json({ message: "An error occurred while searching for doctors." });
    }
};

const getDoctorsList = async (req, res) => {
    try {
        let query = `SELECT DoctorID, Name, Specialization, Qualifications, Availability FROM Doctors`;
        const [doctors] = await pool.execute(query);

        if (doctors.length > 0) {
            return res.status(200).json({ success: true, data: doctors });
        } else {
            return res.status(404).json({success: false, message: "No doctors found matching the search criteria." });
        }
    } catch (error) {
        console.error("Error searching for doctors:", error.message);
        res.status(500).json({success: false, message: "An error occurred while searching for doctors." });
    }
};

const getPharmacies = async (req, res) => {
    try {
        const [pharmacies] = await pool.execute('SELECT PharmacyID, Name, Location, ContactInfo FROM Pharmacy');

        if (pharmacies.length > 0) {
            return res.status(200).json({success: true, pharmacies });
        } else {
            return res.status(404).json({success: false, message: 'No pharmacies found.' });
        }
    } catch (error) {
        console.error('Error fetching pharmacies:', error.message);
        res.status(500).json({success: false, message: 'An error occurred while fetching pharmacies.' });
    }
};


const getMedicalRecord = async (req, res) => {
    try {
        const { patientID } = req.params;

        const [record] = await pool.execute('SELECT * FROM MedicalRecords WHERE PatientID = ?', [patientID]);

        if (record.length === 0) {
            return res.status(200).json({success: false, message: 'Medical record not found or access denied.' });
        }

        return res.status(200).json({
            success: true,
            medicalRecords: record
        });
    } catch (error) {
        console.error('Error fetching medical record:', error.message);
        res.status(500).json({success: false, message: 'An error occurred while fetching the medical record.' });
    }
};


const uploadMedicalRecord = async (req, res) => {
    try {
        let { patientId, fileUrl, medicalRecordName } = req.body;

        if (!patientId || !fileUrl || !medicalRecordName) {
            return res.status(400).json({ success: false, message: 'All fields are required.' });
        }

        const [user] = await pool.execute('SELECT * FROM Patients WHERE PatientID = ?', [patientId]);

        console.log(user)
        if (user.length === 0) {
            return res.status(200).json({ success: false, message: 'Invalid profile' });
        }

        medicalRecordName = `${user[0].Name}_Medical_Record`;

        const query = `
            INSERT INTO MedicalRecords (PatientID, FileUrl, AdminID, MedicalRecordName)
            VALUES (?, ?, ?, ?)
        `;
        console.log(patientId, fileUrl, "123", medicalRecordName)
        await pool.execute(query, [patientId, fileUrl, "123", medicalRecordName]);

        return res.status(201).json({ success: true, message: 'Medical record uploaded successfully.' });
    } catch (error) {
        console.error('Error uploading medical record:', error.message);
        return res.status(500).json({ success: false, message: 'An error occurred while uploading the medical record.' });
    }
};

const deleteMedicalRecord = async (req, res) => {
    try {
        const { recordId, patientId } = req.body;

        const [record] = await pool.execute(
            'SELECT * FROM MedicalRecords WHERE RecordID = ? AND PatientID = ?',
            [recordId, patientId]
        );

        if (record.length === 0) {
            return res.status(404).json({ success: false, message: 'Medical record not found or access denied.' });
        }

        await pool.execute('DELETE FROM MedicalRecords WHERE RecordID = ? AND PatientID = ?', [recordId, patientId]);

        return res.status(200).json({ success: true, message: 'Medical record deleted successfully.' });
    } catch (error) {
        console.error('Error deleting medical record:', error.message);
        return res.status(500).json({ success: false, message: 'An error occurred while deleting the medical record.' });
    }
};



module.exports = { 
    signup, 
    login,
    validate,
    fetchProfile,
    updateProfile,
    verifyEmail,
    scheduleAppointment,
    getAppointmentsByPatient,
    updateAppointmentStatus,
    deleteAppointment,
    searchDoctors,
    getPharmacies,
    getDoctorsList,
    getMedicalRecord,
    uploadMedicalRecord,
    deleteMedicalRecord,
    rescheduleAppointment,
    getDoctor,
    forgotPassword, 
    resetPassword
};