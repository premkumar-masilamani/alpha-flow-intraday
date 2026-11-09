INSERT INTO tickers (ticker_symbol, ticker_name, token, scrip_symbol, country, timezone, is_active)
VALUES ('NIFTY50', 'NIFTY 50', '99926000', 'NIFTY', 'IN', 'Asia/Kolkata', true)
ON CONFLICT (ticker_symbol) DO NOTHING;

INSERT INTO market_holidays (country, holiday_date, name)
VALUES ('IN', '2026-01-15', 'Municipal Corporation Election - Maharashtra'),
       ('IN', '2026-01-26', 'Republic Day'),
       ('IN', '2026-03-03', 'Holi'),
       ('IN', '2026-03-26', 'Shri Ram Navami'),
       ('IN', '2026-03-31', 'Shri Mahavir Jayanti'),
       ('IN', '2026-04-03', 'Good Friday'),
       ('IN', '2026-04-14', 'Dr. Baba Saheb Ambedkar Jayanti'),
       ('IN', '2026-05-01', 'Maharashtra Day'),
       ('IN', '2026-05-28', 'Bakri Id'),
       ('IN', '2026-06-26', 'Muharram'),
       ('IN', '2026-09-14', 'Ganesh Chaturthi'),
       ('IN', '2026-10-02', 'Mahatma Gandhi Jayanti'),
       ('IN', '2026-10-20', 'Dussehra'),
       ('IN', '2026-11-10', 'Diwali-Balipratipada'),
       ('IN', '2026-11-24', 'Prakash Gurpurb Sri Guru Nanak Dev'),
       ('IN', '2026-12-25', 'Christmas')
ON CONFLICT (country, holiday_date) DO NOTHING;
