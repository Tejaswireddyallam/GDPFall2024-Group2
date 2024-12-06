CREATE DATABASE telemedicine_dummy_database;

USE telemedicine_dummy_database;

\\ Creating Patients Table
CREATE TABLE Patients (
    PatientID INTEGER PRIMARY KEY,
    Name VARCHAR(100),
    Email VARCHAR(100),
    Password VARCHAR(255),
    Role VARCHAR(50),
    ContactInfo VARCHAR(100)
    Address VARCHAR(255),
);

\\ Creating Doctors Table
CREATE TABLE Doctors (
    DoctorID INTEGER PRIMARY KEY,
    Name VARCHAR(100),
    Email VARCHAR(100),
    Password VARCHAR(255),
    Role VARCHAR(50),
    ContactInfo VARCHAR(100)
    Specialization VARCHAR(100),
    Qualifications TEXT,
    Availability VARCHAR(50),
);

\\Creating Appointments Table
CREATE TABLE Appointments (
    AppointmentID INTEGER PRIMARY KEY,
    PatientID INTEGER,
    DoctorID INTEGER,
    AdminID INTEGER,
    DateTime DATETIME,
    Status VARCHAR(50),
    FOREIGN KEY (PatientID) REFERENCES Patients(PatientID),
    FOREIGN KEY (DoctorID) REFERENCES Doctors(DoctorID),
    FOREIGN KEY (AdminID) REFERENCES Administration(AdminID)
);
\\Creating Medical Records Table
CREATE TABLE MedicalRecords (
    RecordID INTEGER PRIMARY KEY,
    PatientID INTEGER,
    DoctorID INTEGER,
    AdminID INTEGER,
    ConsultationNotes TEXT,
    UploadFilePath VARCHAR(255),
    FOREIGN KEY (PatientID) REFERENCES Patients(PatientID),
    FOREIGN KEY (DoctorID) REFERENCES Doctors(DoctorID),
    FOREIGN KEY (AdminID) REFERENCES Administration(AdminID)
);
\\Creating Prescriptions Table
CREATE TABLE Prescriptions (
    PrescriptionID INTEGER PRIMARY KEY,
    DoctorID INTEGER,
    PatientID INTEGER,
    AppointmentID INTEGER,
    AdminID INTEGER,
    MedicationDetails TEXT,
    Date DATE,
    Dosage VARCHAR(100),
    FOREIGN KEY (PatientID) REFERENCES Patients(PatientID),
    FOREIGN KEY (DoctorID) REFERENCES Doctors(DoctorID),
    FOREIGN KEY (AppointmentID) REFERENCES Appointments(AppointmentID),
    FOREIGN KEY (AdminID) REFERENCES Administration(AdminID)
   );
\\Creating Messages Table
CREATE TABLE Messages (
    MessageID INTEGER PRIMARY KEY,
    DoctorID INTEGER,
    PatientID INTEGER,
    AdminID INTEGER,
    Content TEXT,
    Timestamp DATETIME,
   FOREIGN KEY (PatientID) REFERENCES Patients(PatientID),
    FOREIGN KEY (DoctorID) REFERENCES Doctors(DoctorID),
    FOREIGN KEY (AdminID) REFERENCES Administration(AdminID)
);
\\Creating Notifications Table
CREATE TABLE Notifications (
    NotificationID INTEGER PRIMARY KEY,
    PatientID INTEGER,
    DoctorID INTEGER,
    AdminID INTEGER,
    Content TEXT,
    FOREIGN KEY (PatientID) REFERENCES Patients(PatientID),
    FOREIGN KEY (DoctorID) REFERENCES Doctors(DoctorID),
    FOREIGN KEY (AdminID) REFERENCES Administration(AdminID)
);
\\Creating Pharmacy Table
CREATE TABLE Pharmacy (
    PharmacyID INTEGER PRIMARY KEY,
    Name VARCHAR(100),
    Location VARCHAR(255),
    ContactInfo VARCHAR(100)
);
\\Creating Administration Table
CREATE TABLE Administration (
    AdminID INTEGER PRIMARY KEY,
    Name VARCHAR(100),
    ContactInfo VARCHAR(100)
);
\\Creating Activity Log Table
CREATE TABLE ActivityLog (
    LogID INTEGER PRIMARY KEY,
    PatientID INTEGER,
    DoctorID INTEGER,
    AdminID INTEGER,
    LogDatetime DATETIME,
    Description TEXT,
    Status VARCHAR(50),
    FOREIGN KEY (PatientID) REFERENCES Patients(PatientID),
    FOREIGN KEY (DoctorID) REFERENCES Doctors(DoctorID),
    FOREIGN KEY (AdminID) REFERENCES Administration(AdminID)
);


-- Inserting data into Patients Table
INSERT INTO Patients (PatientID, Name, Email, Password, Role, ContactInfo, Address) VALUES
(1, 'Alice Johnson', 'alice.johnson@example.com', 'hashedpassword1', 'patient', '555-1234', '123 Maple St.'),
(2, 'Bob Smith', 'bob.smith@example.com', 'hashedpassword2', 'patient', '555-5678', '456 Oak St.'),
(3, 'Carol White', 'carol.white@example.com', 'hashedpassword3', 'patient', '555-7890', '789 Birch St.');

-- Inserting data into Doctors Table
INSERT INTO Doctors (DoctorID, Name, Email, Password, Role, ContactInfo, Specialization, Qualifications, Availability) VALUES
(1, 'Dr. Sarah Lee', 'sarah.lee@example.com', 'hashedpassword4', 'doctor', '555-8765', 'Cardiology', 'MD, Cardiology Specialist', 'Mon-Fri 9AM-5PM'),
(2, 'Dr. Michael Brown', 'michael.brown@example.com', 'hashedpassword5', 'doctor', '555-4321', 'Dermatology', 'MD, Dermatology Expert', 'Tue-Thu 10AM-4PM'),
(3, 'Dr. Emily Davis', 'emily.davis@example.com', 'hashedpassword6', 'doctor', '555-6789', 'Pediatrics', 'MD, Pediatric Specialist', 'Mon-Wed 9AM-1PM');

-- Inserting data into Appointments Table
INSERT INTO Appointments (AppointmentID, PatientID, DoctorID, AdminID, DateTime, Status) VALUES
(1, 1, 1, 1, '2024-11-01 10:00:00', 'Scheduled'),
(2, 2, 2, 1, '2024-11-02 14:00:00', 'Completed'),
(3, 3, 3, 1, '2024-11-03 09:00:00', 'Pending');

-- Inserting data into MedicalRecords Table
INSERT INTO MedicalRecords (RecordID, PatientID, DoctorID, AdminID, ConsultationNotes, UploadFilePath) VALUES
(1, 1, 1, 1, 'Patient shows stable vitals; recommend further tests.', '/records/record1.pdf'),
(2, 2, 2, 1, 'Skin condition improved with treatment.', '/records/record2.pdf'),
(3, 3, 3, 1, 'Routine pediatric check-up completed.', '/records/record3.pdf');

-- Inserting data into Prescriptions Table
INSERT INTO Prescriptions (PrescriptionID, DoctorID, PatientID, AppointmentID, AdminID, MedicationDetails, Date, Dosage) VALUES
(1, 1, 1, 1, 'Ibuprofen 200mg, twice daily for pain relief', '2024-11-01', '200mg'),
(2, 2, 2, 2, 'Topical ointment, apply twice daily to affected area', '2024-11-02', 'Apply twice daily'),
(3, 3, 3, 3, 'Amoxicillin 250mg, three times a day for infection', '2024-11-03', '250mg');

-- Inserting data into Messages Table
INSERT INTO Messages (MessageID, DoctorID, PatientID, AdminID, Content, Timestamp) VALUES
(1, 1, 1, 1, 'Your appointment is confirmed for 2024-11-01.', '2024-10-25 10:00:00'),
(2, 2, 2, 1, 'Please review the treatment plan.', '2024-10-26 12:00:00'),
(3, 3, 3, 1, 'Follow-up scheduled for next week.', '2024-10-27 14:30:00');

-- Inserting data into Notifications Table
INSERT INTO Notifications (NotificationID, PatientID, DoctorID, AdminID, Content) VALUES
(1, 1, 1, 1, 'Appointment reminder for 2024-11-01'),
(2, 2, 2, 1, 'Treatment plan available for review'),
(3, 3, 3, 1, 'Follow-up scheduled for next week');

-- Inserting data into Pharmacy Table
INSERT INTO Pharmacy (PharmacyID, Name, Location, ContactInfo) VALUES
(1, 'Health First Pharmacy', '101 Main St.', '555-1234'),
(2, 'Wellness Drug Store', '202 South St.', '555-5678'),
(3, 'CarePlus Pharmacy', '303 West Ave.', '555-7890');

-- Inserting data into Administration Table
INSERT INTO Administration (AdminID, Name, ContactInfo) VALUES
(1, 'John Doe', '555-9999'),
(2, 'Anna Bell', '555-8888'),
(3, 'Samuel Grant', '555-7777');

-- Inserting data into ActivityLog Table
INSERT INTO ActivityLog (LogID, PatientID, DoctorID, AdminID, LogDatetime, Description, Status) VALUES
(1, 1, 1, 1, '2024-10-25 09:00:00', 'Scheduled appointment', 'Completed'),
(2, 2, 2, 1, '2024-10-26 11:30:00', 'Checked prescription details', 'Pending'),
(3, 3, 3, 1, '2024-10-27 13:45:00', 'Updated medical record', 'In Progress');

\\Verification of Data
SELECT * FROM Doctors;
SELECT * FROM Patients;
SELECT * FROM Appointments;
SELECT * FROM MedicalRecords;
SELECT * FROM Prescriptions;
SELECT * FROM Messages;
SELECT * FROM Notifications;
SELECT * FROM Pharmacy;
SELECT * FROM Administration;
SELECT * FROM ActivityLog;
