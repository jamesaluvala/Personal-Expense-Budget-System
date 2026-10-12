import { useCallback, useEffect, useState } from 'react';
import { api } from './api.js';
import { errorText } from './format.js';
export function useResource(path, revision = 0) {
  const [retry, setRetry] = useState(0);
  const [state, setState] = useState({ path, data: null, loading: true, error: '' });
  useEffect(() => {
    if (!path) return;
    const controller = new AbortController();
    let alive = true;
    setState(old => ({ path, data: old.path === path ? old.data : null, loading: true, error: '' }));
    api(path, { signal: controller.signal }).then(data => {
      if (alive) setState({ path, data, loading: false, error: '' });
    }).catch(error => {
      if (alive && error.name !== 'AbortError') setState(old => ({ ...old, loading: false, error: errorText(error) }));
    });
    return () => { alive = false; controller.abort(); };
  }, [path, revision, retry]);
  const reload = useCallback(() => setRetry(n => n + 1), []);
  return { ...state, data: state.path === path ? state.data : null, reload };
}
export function useDebounce(value, delay = 300) {
  const [result, setResult] = useState(value);
  useEffect(() => { const timer = setTimeout(() => setResult(value), delay); return () => clearTimeout(timer); }, [value, delay]);
  return result;
}
