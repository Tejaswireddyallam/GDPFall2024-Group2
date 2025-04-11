const jwt = require("jsonwebtoken");
const bcrypt = require('bcrypt');
const pool = require("../configs/pool.js");
const path = require('path');
const dotenv = require('dotenv').config();
const emails = require('../emails/mails.js');
const crypto = require('crypto');

var savedOTPS = {
};

const signup = async (req, res) => {
    try {
        console.log(req.body)
        const { fullname, email, password, contact, specialization, qualification, availability, role } = req.body;

        if (!fullname || !email || !password) {
            return res.status(400).json({ message: 'Please provide all required fields.' });
        }

        const [existingUser] = await pool.execute('SELECT * FROM Doctors WHERE Email = ?', [email]);
        if (existingUser.length > 0) {
            return res.status(409).json({ message: 'Email is already registered.' });
        }
        const hashedPassword = await bcrypt.hash(password, 10);
        const insertQuery = `
            INSERT INTO Doctors 
            (Name, Email, Password, Role, ContactInfo, Specialization, Qualifications, Availability, IsVerified, Gender, DOB) 
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)`;
        const [result] = await pool.execute(insertQuery, [
            fullname,
            email,
            hashedPassword,
            role,
            contact,
            specialization,
            qualification,
            availability,
            0,
            0,
            0
        ]);

        if (result.affectedRows > 0) {
            const token = jwt.sign({ email }, process.env.JWT_TOKEN_KEY, { expiresIn: '1h' });

            const verificationUrl = `http://localhost:8080/api/doctor/verify-email?token=${token}`;
            emails.sendVerificationEmail(email, verificationUrl);

            return res.status(201).json({ message: 'Registration successful. Please verify your email.' });
        } else {
            return res.status(500).json({ message: 'Registration Failed.' });
        }
    } catch (error) {
        console.error('Error during signup:', error.message);
        res.status(500).json({ message: 'An error occurred during registration.' });
    }
};

const login = async (req, res) => {
    try {
        console.log("Doctor Login -> ", req.body);
        const { email, password } = req.body;
        if (!email || !password) {
            return res.status(400).json({ message: 'Please provide both email and password.' });
        }

        const [user] = await pool.execute('SELECT * FROM Doctors WHERE Email = ?', [email]);
        console.log(user);
        if (user.length === 0) {
            return res.status(200).json({ success: false, message: 'Invalid email or password.' });
        }

        const foundUser = user[0];
        const isPasswordValid = await bcrypt.compare(password, foundUser.Password);
        if (!isPasswordValid) {
            return res.status(401).json({ success: false, message: 'Invalid email or password.' });
        }

        const tokenPayload = { id: foundUser.DoctorID, name: foundUser.Name };
        const token = jwt.sign(tokenPayload, process.env.JWT_TOKEN_KEY, { expiresIn: '7d' });

        const userResponse = {
            id: foundUser.DoctorID,
            name: foundUser.Name,
            email: foundUser.Email,
            role: 'Doctor',
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
        res.status(500).json({ success: false, message: 'An error occurred during login.' });
    }
};


const forgotPassword = async (req, res) => {
    try {
        console.log("Forgot Password -> ", req.body);
        const { email } = req.body;

        if (!email) {
            return res.status(400).json({ success: false, message: "Please provide an email." });
        }

        const [user] = await pool.execute("SELECT * FROM Doctors WHERE Email = ?", [email]);
        if (user.length === 0) {
            return res.status(404).json({ success: false, message: "Email not found." });
        }

        const resetToken = crypto.randomBytes(32).toString("hex");
        const hashedToken = await bcrypt.hash(resetToken, 10);
        const expiresAt = new Date(Date.now() + 30 * 60 * 1000);


        await pool.execute(
            "UPDATE Doctors SET ResetToken = ?, ResetTokenExpires = ? WHERE Email = ?",
            [hashedToken, expiresAt, email]
        );

        const resetUrl = `${process.env.IP}/api/doctor/reset-password?token=${resetToken}&email=${email}`;

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

        const [user] = await pool.execute("SELECT * FROM Doctors WHERE Email = ?", [email]);
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
            "UPDATE Doctors SET Password = ?, ResetToken = NULL, ResetTokenExpires = NULL WHERE Email = ?",
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
        console.log("Doctor Profile -> ", req.body);
        const { email } = req.body;
        if (!email) {
            return res.status(400).json({ message: 'Please provide both email' });
        }

        const [user] = await pool.execute('SELECT * FROM Doctors WHERE Email = ?', [email]);
        console.log(user)
        if (user.length === 0) {
            return res.status(200).json({ success: false, message: 'Invalid email' });
        }

        const foundUser = user[0];
        const userResponse = {
            name: foundUser.Name,
            email: foundUser.Email,
            contact: foundUser.ContactInfo,
            gender: foundUser.Gender,
            dob: foundUser.DOB,
            availability: foundUser.Availability,
            specialization: foundUser.Specialization,
            qualification: foundUser.Qualifications
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

const updateProfile = async (req, res) => {
    try {
        const { fullname, email, contact, availability, specialization, qualification, gender, dob } = req.body;

        if (!email) {
            return res.status(400).json({ message: 'Email is required.' });
        }

        const updateQuery = `
            UPDATE Doctors 
            SET Name = ?, ContactInfo = ?, Availability = ?, Specialization = ?, Qualifications = ?, Gender = ?, DOB = ?
            WHERE Email = ?
        `;

        const [result] = await pool.execute(updateQuery, [fullname, contact, availability, specialization, qualification, gender, dob, email]);

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

const validate = async (req, res) => {
    try {
        console.log("Token Validation -> ", req.body);
        const { email, token } = req.body;

        if (!email || !token) {
            return res.status(400).json({ message: 'Please provide both email and token.' });
        }

        const [user] = await pool.execute('SELECT * FROM Doctors WHERE Email = ?', [email]);
        if (user.length === 0) {
            return res.status(404).json({ success: false, message: 'User not found.' });
        }

        const foundUser = user[0];

        try {
            const decodedToken = jwt.verify(token, process.env.JWT_TOKEN_KEY);

            if (decodedToken.id !== foundUser.DoctorID || decodedToken.name !== foundUser.Name) {
                return res.status(401).json({ success: false, message: 'Invalid token.' });
            }

            const userResponse = {
                id: foundUser.DoctorID,
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

        const [result] = await pool.execute('UPDATE Doctors SET IsVerified = ? WHERE Email = ?', [true, decoded.email]);

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

const getAppointmentsByDoctor = async (req, res) => {
    try {
        const { doctorID } = req.params;

        if (!doctorID) {
            return res.status(400).json({success: false, message: "Doctor ID is required." });
        }

        const query = `
            SELECT a.AppointmentID, a.SlotDate, a.SlotTime, a.Status, 
                   p.PatientID, p.Name as PatientName 
            FROM Appointments a
            JOIN Patients p ON a.PatientID = p.PatientID
            WHERE a.DoctorID = ? 
            ORDER BY a.SlotDate ASC
        `;
        const [appointments] = await pool.execute(query, [doctorID]);

        if (appointments.length > 0) {
            return res.status(200).json({success: true, appointments });
        } else {
            return res.status(404).json({success: false, message: "No appointments found for this doctor." });
        }
    } catch (error) {
        console.error("Error retrieving doctor appointments:", error.message);
        res.status(500).json({success: false, message: "An error occurred while retrieving appointments." });
    }
};

const updateAppointmentStatusByDoctor = async (req, res) => {
    try {
        console.log("Updating Appointment Status -> ", req.body);
        const { appointmentID, status } = req.body;

        if (!appointmentID || !status) {
            return res.status(400).json({ success: false, message: "Please provide both appointment ID and status." });
        }

        const updateQuery = `UPDATE Appointments SET Status = ? WHERE AppointmentID = ?`;
        const [updateResult] = await pool.execute(updateQuery, [status, appointmentID]);

        if (updateResult.affectedRows > 0) {

            const fetchQuery = `
                SELECT 
                    a.AppointmentID, 
                    a.Status, 
                    a.SlotDate AS appointmentDate,
                    a.SlotTime AS appointmentTime,
                    p.PatientID,
                    p.Name AS patientName,
                    p.Email AS patientEmail,
                    d.DoctorID,
                    d.Name AS doctorName,
                    d.Specialization AS doctorSpec,
                    d.Availability AS doctorAvail
                FROM Appointments a
                JOIN Patients p ON a.PatientID = p.PatientID
                JOIN Doctors d ON a.DoctorID = d.DoctorID
                WHERE a.AppointmentID = ?`;

            const [appointmentData] = await pool.execute(fetchQuery, [appointmentID]);

            if (appointmentData.length === 0) {
                return res.status(404).json({ success: false, message: "Appointment not found after update." });
            }

            const updatedAppointment = appointmentData[0];
            console.log(updatedAppointment);

            let text = "";

            if(status == 'cancelled'){
                text = `Dear ${updatedAppointment.patientName}, your appointment scheduled for Date - ${updatedAppointment.appointmentDate} & Time : ${updatedAppointment.appointmentTime} with Dr.${updatedAppointment.doctorName} ( ${updatedAppointment.doctorSpec} ) has been cancelled. Try rescheduling in between ${updatedAppointment.doctorAvail}. Thank You.`
            } else if (status == 'confirmed') {
                text = `Dear ${updatedAppointment.patientName}, your appointment scheduled for Date - ${updatedAppointment.appointmentDate} & Time : ${updatedAppointment.appointmentTime} with Dr.${updatedAppointment.doctorName} ( ${updatedAppointment.doctorSpec} ) has been confimed. Thank You.`
            }
           
                emails.sendAppointmentUpdate(updatedAppointment.patientEmail, status, text);

            return res.status(200).json({
                success: true,
                message: "Appointment status updated successfully.",
            });
        } else {
            return res.status(404).json({ success: false, message: "Appointment not found." });
        }
    } catch (error) {
        console.error("Error updating appointment status:", error);
        res.status(500).json({ success: false, message: "An error occurred while updating the appointment status." });
    }
};

const getAppointmentDetails = async (req, res) => {
    try {
        const { appointmentID } = req.params;

        if (!appointmentID) {
            return res.status(400).json({ message: "Appointment ID is required." });
        }

        const query = `
            SELECT a.AppointmentID, a.DateTime, a.Status, 
                   p.PatientID, p.Name as PatientName, p.ContactInfo, p.Email,
                   d.Name as DoctorName, d.ContactInfo as DoctorContact, d.Email as DoctorEmail,
                   ad.Name as AdminName, ad.ContactInfo as AdminContact
            FROM Appointments a
            LEFT JOIN Patients p ON a.PatientID = p.PatientID
            LEFT JOIN Doctors d ON a.DoctorID = d.DoctorID
            LEFT JOIN Administration ad ON a.AdminID = ad.AdminID
            WHERE a.AppointmentID = ?
        `;
        const [details] = await pool.execute(query, [appointmentID]);

        if (details.length > 0) {
            return res.status(200).json({ appointmentDetails: details[0] });
        } else {
            return res.status(404).json({ message: "Appointment not found." });
        }
    } catch (error) {
        console.error("Error retrieving appointment details:", error.message);
        res.status(500).json({ message: "An error occurred while retrieving appointment details." });
    }
};

const cancelAppointmentByDoctor = async (req, res) => {
    try {
        const { appointmentID } = req.params;

        if (!appointmentID) {
            return res.status(400).json({ message: "Appointment ID is required." });
        }

        const query = `UPDATE Appointments SET Status = 'Cancelled' WHERE AppointmentID = ?`;
        const [result] = await pool.execute(query, [appointmentID]);

        if (result.affectedRows > 0) {
            return res.status(200).json({ message: "Appointment cancelled successfully." });
        } else {
            return res.status(404).json({ message: "Appointment not found." });
        }
    } catch (error) {
        console.error("Error cancelling appointment:", error.message);
        res.status(500).json({ message: "An error occurred while cancelling the appointment." });
    }
};

const createMedicalRecord = async (req, res) => {
    try {
        const { patientID, doctorID, adminID, consultationNotes, uploadFilePath } = req.body;

        if (!patientID || !doctorID || !adminID || !consultationNotes) {
            return res.status(400).json({ message: 'Please provide all required fields.' });
        }

        const encryptedNotes = crypto.createCipher('aes-256-cbc', process.env.ENCRYPTION_KEY).update(consultationNotes, 'utf8', 'hex') + crypto.createCipher('aes-256-cbc', process.env.ENCRYPTION_KEY).final('hex');

        const insertQuery = 'INSERT INTO MedicalRecords (PatientID, DoctorID, AdminID, ConsultationNotes, UploadFilePath) VALUES (?, ?, ?, ?, ?)';
        const [result] = await pool.execute(insertQuery, [patientID, doctorID, adminID, encryptedNotes, uploadFilePath]);

        if (result.affectedRows > 0) {
            return res.status(201).json({ message: 'Medical record created successfully.' });
        } else {
            return res.status(500).json({ message: 'Failed to create medical record.' });
        }
    } catch (error) {
        console.error('Error creating medical record:', error.message);
        res.status(500).json({ message: 'An error occurred while creating the medical record.' });
    }
};

const getMedicalRecord = async (req, res) => {
    try {
        const { recordID, patientID } = req.params;

        const [record] = await pool.execute('SELECT * FROM MedicalRecords WHERE RecordID = ? AND PatientID = ?', [recordID, patientID]);

        if (record.length === 0) {
            return res.status(404).json({ message: 'Medical record not found or access denied.' });
        }

        const decryptedNotes = crypto.createDecipher('aes-256-cbc', process.env.ENCRYPTION_KEY).update(record[0].ConsultationNotes, 'hex', 'utf8') + crypto.createDecipher('aes-256-cbc', process.env.ENCRYPTION_KEY).final('utf8');

        return res.status(200).json({
            recordID: record[0].RecordID,
            doctorID: record[0].DoctorID,
            adminID: record[0].AdminID,
            consultationNotes: decryptedNotes,
            uploadFilePath: record[0].UploadFilePath,
        });
    } catch (error) {
        console.error('Error fetching medical record:', error.message);
        res.status(500).json({ message: 'An error occurred while fetching the medical record.' });
    }
};

const rescheduleAppointment = async (req, res) => {
    try {
        console.log("Doctor Reschedule Appointment -> ", req.body);
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

const getPatient = async (req, res) => {
    try {
        const { patientId } = req.params;
        console.log(patientId)
        if (!patientId) {
            return res.status(400).json({success: false, message: "Please provide patient id to search." });
        }

        let query = `SELECT Name FROM Patients WHERE PatientID = ?`;
   
        const [patients] = await pool.execute(query, [patientId]);

        if (patients.length > 0) {
            return res.status(200).json({success: true, data : patients });
        } else {
            return res.status(404).json({success: false, message: "No doctors found matching the search criteria." });
        }
    } catch (error) {
        console.error("Error searching for doctors:", error.message);
        res.status(500).json({ message: "An error occurred while searching for doctors." });
    }
};

const getPatients = async (req, res) => {
    try {

        let query = `SELECT PatientID, Name FROM Patients`;
   
        const [patients] = await pool.execute(query);

        if (patients.length > 0) {
            return res.status(200).json({success: true, patients });
        } else {
            return res.status(404).json({success: false, message: "No patients found matching the search criteria." });
        }
    } catch (error) {
        console.error("Error searching for patients:", error.message);
        res.status(500).json({ message: "An error occurred while searching for patients." });
    }
};

module.exports = { 
    signup, 
    login,
    forgotPassword,
    resetPassword,
    verifyEmail,
    validate,
    fetchProfile,
    updateProfile,
    getAppointmentsByDoctor,
    updateAppointmentStatusByDoctor,
    getAppointmentDetails,
    cancelAppointmentByDoctor,
    createMedicalRecord, 
    getMedicalRecord,
    getPatient,
    getPatients
};