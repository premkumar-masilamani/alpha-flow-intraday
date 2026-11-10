import { render, screen, waitFor } from '@testing-library/react';
import { describe, it, expect, vi, beforeEach } from 'vitest';
import Header from './Header';
import * as api from '../services/api';

vi.mock('../services/api', () => ({
    getCachedMarketHolidays: vi.fn(),
}));

describe('Header Component', () => {
    beforeEach(() => {
        vi.clearAllMocks();
    });

    it('renders the header with the title, calendar, clock, and market status badge', async () => {
        vi.mocked(api.getCachedMarketHolidays).mockResolvedValueOnce([
            {
                id: 1,
                country: 'IN',
                holidayDate: '2026-01-01',
                name: 'New Year’s Day',
            },
        ]);

        render(<Header />);

        const titleElement = screen.getByText('Alpha Flow Intraday');
        expect(titleElement).toBeInTheDocument();
        expect(titleElement).toHaveAttribute('id', 'app-title');

        const marketStatus = screen.getByText(/Market (Open|Closed)/i);
        expect(marketStatus).toBeInTheDocument();

        // Old dual badges should be removed
        expect(screen.queryByText(/NSE:/i)).not.toBeInTheDocument();
        expect(screen.queryByText(/NYSE:/i)).not.toBeInTheDocument();

        await waitFor(() => {
            expect(api.getCachedMarketHolidays).toHaveBeenCalledTimes(1);
        });
    });

    it('gracefully handles holiday fetch failure', async () => {
        vi.mocked(api.getCachedMarketHolidays).mockRejectedValueOnce(new Error('Network error'));

        render(<Header />);

        const titleElement = screen.getByText('Alpha Flow Intraday');
        expect(titleElement).toBeInTheDocument();

        const marketStatus = screen.getByText(/Market (Open|Closed)/i);
        expect(marketStatus).toBeInTheDocument();
    });
});
