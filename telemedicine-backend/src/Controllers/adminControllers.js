const jwt = require("jsonwebtoken");
const bcrypt = require('bcrypt');
const pool = require("../configs/pool.js");
const path = require('path');
const dotenv = require('dotenv').config();
const emails = require('../emails/mails.js');
const crypto = require('crypto');

var savedOTPS = {
};


const adminLogin = (req, res) => {
    const { email, password } = req.body;

    const defaultEmail = 'admin123@gmail.com';
    const defaultPassword = '#admin123';

    if (email === defaultEmail && password === defaultPassword) {
        return res.json({ success: true, message: 'Login successful' });
    } else {
        return res.status(401).json({ success: false, message: 'Invalid credentials' });
    }
};


const updateActivityStatus = async (req, res) => {
    const { logId } = req.params;
    const { status } = req.body;

    try {
        const [result] = await pool.query(
            `UPDATE ActivityLog SET Status = ? WHERE LogID = ?`,
            [status, logId]
        );

        if (result.affectedRows === 0) {
            return res.status(404).json({ message: 'Log not found' });
        }

        res.json({ message: 'Status updated successfully' });
    } catch (err) {
        res.status(500).json({ error: err.message });
    }
};

/*

const getAllActivityLogs = async (req, res) => {
    try {
        const [results] = await pool.query(`SELECT * FROM ActivityLog ORDER BY LogDatetime DESC`);
        res.json(results);
    } catch (err) {
        res.status(500).json({ error: err.message });
    }
};


*/


const getAllActivityLogs = async (req, res) => {
    try {
        let { page = 1, limit = 10, role, date, sort = 'desc' } = req.query;
        page = parseInt(page);
        limit = parseInt(limit);
        const offset = (page - 1) * limit;

        let baseQuery = `SELECT * FROM ActivityLog WHERE 1=1`;
        const params = [];

        if (role) {
            baseQuery += ` AND Role = ?`;
            params.push(role);
        }

        if (date) {
            baseQuery += ` AND DATE(LogDatetime) = ?`;
            params.push(date);
        }

        baseQuery += ` ORDER BY LogDatetime ${sort === 'asc' ? 'ASC' : 'DESC'} LIMIT ? OFFSET ?`;
        params.push(limit, offset);

        const [results] = await pool.query(baseQuery, params);
        res.json(results);
    } catch (err) {
        res.status(500).json({ error: err.message });
    }
};


module.exports = { adminLogin, updateActivityStatus, getAllActivityLogs };
