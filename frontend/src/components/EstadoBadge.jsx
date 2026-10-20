export default function EstadoBadge({ estado }) {
  return <span className={`badge ${estado}`}>{estado}</span>;
}
