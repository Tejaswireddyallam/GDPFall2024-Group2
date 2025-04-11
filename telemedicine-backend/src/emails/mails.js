const nodemailer = require("nodemailer");
const dotenv = require("dotenv").config();

const sendPasswordResetLink = async (mail, link) => {
    try {
        const passwordMsg = {
            from: "telemedicineappproj@gmail.com",
            to: mail,
            subject: `Password Reset Link`,
            html: `<div><h4>Hi, Here is the link to reset the password for your account in Telemedicine. Kindly reset the password. Note: The link is only valid for fifteen minutes after which it expires.</h4><a href=${link}>Password reset link</a> <p>If you didn't request this, please ignore this email.</p></div>`
        };

        const transporter = nodemailer.createTransport({
            service: "gmail",
            auth: {
                user: "telemedicineappproj@gmail.com",
                pass: "plrclluxhtqrjjfe"
            }
        });

        await transporter.sendMail(passwordMsg);
        console.log("Password reset email sent successfully");
    } catch (err) {
        console.error("Error sending password reset email:", err.message);
        throw new Error("Failed to send password reset email");
    }
};

const sendVerificationEmail = async (email, verificationUrl) => {
    try {
        const transporter = nodemailer.createTransport({
            service: 'gmail',
            auth: {
                user: 'telemedicineappproj@gmail.com',
                pass: 'plrclluxhtqrjjfe',
            },
        });

        const mailOptions = {
            from: 'telemedicineappproj@gmail.com',
            to: email,
            subject: 'Verify Your Email',
            html: `<p>Hello! Please verify your email by clicking the link below:</p>
                   <a href="${verificationUrl}">Verify Email</a>`,
        };

        await transporter.sendMail(mailOptions);
        console.log("Verification email sent successfully");
    } catch (err) {
        console.error("Error sending verification email:", err.message);
        throw new Error("Failed to send verification email");
    }
};

const sendAppointmentUpdate = async (patientEmail, status, text) => {
    try {
        const transporter = nodemailer.createTransport({
            service: 'gmail',
            auth: {
                user: 'telemedicineappproj@gmail.com',
                pass: 'plrclluxhtqrjjfe',
            },
        });

        const mailOptions = {
            from: 'telemedicineappproj@gmail.com',
            to: patientEmail,
            subject: `Appointment ${status}`,
            html: text,
        };

        await transporter.sendMail(mailOptions);
        console.log("Appointment update email sent successfully");
    } catch (err) {
        console.error("Error sending appointment update email:", err.message);
        throw new Error("Failed to send appointment update email");
    }
};

const sendAppointmentRequest = async (email, status, text) => {
    try {
        const transporter = nodemailer.createTransport({
            service: 'gmail',
            auth: {
                user: 'telemedicineappproj@gmail.com',
                pass: 'plrclluxhtqrjjfe',
            },
        });

        const mailOptions = {
            from: 'telemedicineappproj@gmail.com',
            to: email,
            subject: `Appointment Request`,
            html: text,
        };

        await transporter.sendMail(mailOptions);
        console.log("Appointment request email sent successfully");
    } catch (err) {
        console.error("Error sending appointment request email:", err.message);
        throw new Error("Failed to send appointment request email");
    }
};

const sendRescheduleAppointment = async (email, status, text) => {
    try {
        const transporter = nodemailer.createTransport({
            service: 'gmail',
            auth: {
                user: 'telemedicineappproj@gmail.com',
                pass: 'plrclluxhtqrjjfe',
            },
        });

        const mailOptions = {
            from: 'telemedicineappproj@gmail.com',
            to: email,
            subject: `Appointment Reschedule Request`,
            html: text,
        };

        await transporter.sendMail(mailOptions);
        console.log("Reschedule appointment email sent successfully");
    } catch (err) {
        console.error("Error sending reschedule appointment email:", err.message);
        throw new Error("Failed to send reschedule appointment email");
    }
};

module.exports = {
    sendVerificationEmail,
    sendPasswordResetLink,
    sendAppointmentUpdate,
    sendAppointmentRequest,
    sendRescheduleAppointment
};
