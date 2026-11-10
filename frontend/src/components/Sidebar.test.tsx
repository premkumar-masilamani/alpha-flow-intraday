import { render, screen, fireEvent } from '@testing-library/react';
import { describe, it, expect, vi } from 'vitest';
import Sidebar from './Sidebar';
import type { Ticker } from '../services/api';

describe('Sidebar Component', () => {
    const mockTickers: Ticker[] = [
        { id: 1, symbol: 'NIFTY50', name: 'NIFTY 50' },
        { id: 2, symbol: 'BANKNIFTY', name: 'BANK NIFTY' },
    ];

    it('renders a simple flat list of tickers without category headers', () => {
        render(
            <Sidebar
                tickers={mockTickers}
                selectedTicker={null}
                onSelectTicker={vi.fn()}
            />
        );

        // Header
        expect(screen.getByText('Tickers')).toBeInTheDocument();

        // No accordion/category headers
        expect(screen.queryByText(/IN - STOCK/i)).not.toBeInTheDocument();
        expect(screen.queryByText(/IN - INDEX/i)).not.toBeInTheDocument();

        // All tickers directly visible
        mockTickers.forEach((ticker) => {
            expect(screen.getByText(ticker.symbol)).toBeInTheDocument();
            expect(screen.getByText(ticker.name)).toBeInTheDocument();
        });
    });

    it('renders "None available" when ticker list is empty', () => {
        render(
            <Sidebar
                tickers={[]}
                selectedTicker={null}
                onSelectTicker={vi.fn()}
            />
        );

        expect(screen.getByText('None available')).toBeInTheDocument();
    });

    it('applies selected classes to the active ticker', () => {
        render(
            <Sidebar
                tickers={mockTickers}
                selectedTicker="NIFTY50"
                onSelectTicker={vi.fn()}
            />
        );

        const niftyButton = screen.getByText('NIFTY50').closest('button');
        const bankNiftyButton = screen.getByText('BANKNIFTY').closest('button');

        expect(niftyButton).toHaveClass('bg-slate-700', 'text-white', 'border-l-4', 'border-blue-500');
        expect(bankNiftyButton).not.toHaveClass('bg-slate-700', 'text-white', 'border-l-4', 'border-blue-500');
        expect(bankNiftyButton).toHaveClass('pl-5');
    });

    it('calls onSelectTicker when a ticker is clicked', () => {
        const handleSelectTicker = vi.fn();
        render(
            <Sidebar
                tickers={mockTickers}
                selectedTicker={null}
                onSelectTicker={handleSelectTicker}
            />
        );

        const bankNiftyButton = screen.getByText('BANKNIFTY').closest('button');
        expect(bankNiftyButton).toBeInTheDocument();
        fireEvent.click(bankNiftyButton!);
        expect(handleSelectTicker).toHaveBeenCalledWith('BANKNIFTY');
    });
});
