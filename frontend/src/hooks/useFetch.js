import { useEffect, useState } from 'react';

/**
 * Runs an async fetcher on mount and whenever `deps` change, tracking loading and error state.
 *
 * @param {Function} fetchFn async function returning the data to store
 * @param {Array} deps dependency list; changing any value refetches
 * @param {string} errorMessage fallback message when the failure carries no server message
 * @returns {{ data: *, loading: boolean, error: string, refetch: Function }}
 */
export default function useFetch(fetchFn, deps = [], errorMessage = 'Failed to load data') {
  const [state, setState] = useState({ data: null, loading: true, error: '' });
  const [reloadKey, setReloadKey] = useState(0);

  useEffect(() => {
    let cancelled = false;

    const load = async () => {
      try {
        const data = await fetchFn();
        if (!cancelled) setState({ data, loading: false, error: '' });
      } catch (err) {
        // Keep previously loaded data visible so a failed refetch does not blank the page.
        if (!cancelled) {
          setState((prev) => ({
            ...prev,
            loading: false,
            error: err.response?.data?.message || errorMessage,
          }));
        }
      }
    };

    load();
    return () => {
      cancelled = true;
    };
    // fetchFn is intentionally excluded: callers often pass an inline arrow.
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [...deps, reloadKey]);

  return { ...state, refetch: () => setReloadKey((key) => key + 1) };
}
