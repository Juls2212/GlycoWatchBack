import { Card } from "@/components/ui/card";
import { MeasurementRowActions } from "@/features/measurements/components/measurement-row-actions";
import { MeasurementItem } from "@/features/measurements/types";

type Props = {
  measurements: MeasurementItem[];
  isLoading: boolean;
  error: string | null;
  deletingId: number | null;
  onDelete: (measurementId: number) => Promise<void>;
};

function resolveOrigin(item: MeasurementItem): string {
  if (item.origin) return item.origin;
  return item.deviceId ? "IOT" : "MANUAL";
}

export function MeasurementsTable({ measurements, isLoading, error, deletingId, onDelete }: Props) {
  return (
    <Card>
      {isLoading ? <p className="soft-text">Cargando mediciones...</p> : null}
      {error ? <p className="error-text">{error}</p> : null}

      {!isLoading && !error && measurements.length === 0 ? (
        <p className="soft-text">No hay mediciones para mostrar.</p>
      ) : null}

      {!isLoading && !error && measurements.length > 0 ? (
        <div className="table-wrap">
          <table className="measurements-table">
            <thead>
              <tr>
                <th>Glucosa</th>
                <th>Unidad</th>
                <th>Fecha</th>
                <th>Origen</th>
                <th>Acción</th>
              </tr>
            </thead>
            <tbody>
              {measurements.map((item) => (
                <tr key={item.id}>
                  <td>{item.glucoseValue}</td>
                  <td>{item.unit}</td>
                  <td>{new Date(item.measuredAt).toLocaleString("es-CO")}</td>
                  <td>{resolveOrigin(item)}</td>
                  <td>
                    <MeasurementRowActions
                      measurementId={item.id}
                      isDeleting={deletingId === item.id}
                      onDelete={onDelete}
                    />
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      ) : null}
    </Card>
  );
}
