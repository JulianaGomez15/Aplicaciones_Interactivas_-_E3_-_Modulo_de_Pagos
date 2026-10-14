export default function ErrorBanner({error}) {
    if (!error) return null;
    return (
        <div className="banner error" role="alert">
            <strong>{error.code ? `${error.code}` : 'Error'}</strong>
            {error.status ? ` (HTTP ${error.status})` : ''}: {error.message}
            {error.correlationId && <small> · correlationId {error.correlationId}</small>}
        </div>
    );
}
