ALTER TABLE calendar_event ADD COLUMN recurrence_freq VARCHAR(10);
ALTER TABLE calendar_event ADD COLUMN recurrence_interval INTEGER;
ALTER TABLE calendar_event ADD COLUMN recurrence_until DATE;
