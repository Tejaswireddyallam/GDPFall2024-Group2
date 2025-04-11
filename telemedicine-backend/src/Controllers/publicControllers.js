const jwt = require("jsonwebtoken");
const bcrypt = require('bcrypt');
const pool = require("../configs/pool.js");
const path = require('path');
const dotenv = require('dotenv').config()
const crypto = require('crypto');
const emails = require('../emails/mails.js')


const addActivityLog = async (req, res) => {
    const { userId, logDatetime, description, role } = req.body;

    const sql = `
        INSERT INTO ActivityLog (UserID, LogDatetime, Description, Role)
        VALUES (?, ?, ?, ?)
    `;

    try {
        const [result] = await pool.execute(sql, [userId, logDatetime, description, role]);
        res.status(201).json({ message: 'Activity log added', logId: result.insertId });
    } catch (err) {
        res.status(500).json({ error: err.message });
    }
};



module.exports = {
    addActivityLog
}