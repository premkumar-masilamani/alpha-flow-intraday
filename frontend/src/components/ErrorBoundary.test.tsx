import { render, screen, fireEvent } from '@testing-library/react';
import { describe, it, expect, vi, beforeEach } from 'vitest';
import ErrorBoundary from './ErrorBoundary';

const CrashingComponent = () => {
  throw new Error('Test crash simulated');
};

const HealthyComponent = () => <div>All systems operational</div>;

describe('ErrorBoundary Component', () => {
  beforeEach(() => {
    vi.spyOn(console, 'error').mockImplementation(() => {});
  });

  it('renders children when no error occurs', () => {
    render(
      <ErrorBoundary>
        <HealthyComponent />
      </ErrorBoundary>
    );

    expect(screen.getByText('All systems operational')).toBeInTheDocument();
  });

  it('renders graceful error fallback UI when a child crashes', () => {
    render(
      <ErrorBoundary fallbackTitle="Custom Crash Title">
        <CrashingComponent />
      </ErrorBoundary>
    );

    expect(screen.getByText('Custom Crash Title')).toBeInTheDocument();
    expect(screen.getByText('Test crash simulated')).toBeInTheDocument();
    expect(screen.getByRole('button', { name: /reload/i })).toBeInTheDocument();
  });

  it('invokes onReset handler when provided upon button click', () => {
    const handleReset = vi.fn();
    render(
      <ErrorBoundary fallbackTitle="Crash" onReset={handleReset}>
        <CrashingComponent />
      </ErrorBoundary>
    );

    const retryBtn = screen.getByRole('button', { name: /try again/i });
    fireEvent.click(retryBtn);
    expect(handleReset).toHaveBeenCalledTimes(1);
  });
});
