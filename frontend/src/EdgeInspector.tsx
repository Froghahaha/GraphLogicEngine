import React from 'react';
import type { Edge, Node } from '@xyflow/react';
import { getAllowedEdgeRoles, type NodeKind } from './nodeSchemas';

type Props = {
    edge: Edge | null;
    sourceNode?: Node | null;
    onChange: (id: string, patch: Record<string, any>) => void;
};

const EdgeInspector: React.FC<Props> = ({ edge, sourceNode, onChange }) => {
    if (!edge) {
        return (
            <div style={{ fontSize: '18px' }}>
                No edge selected
            </div>
        );
    }

    const data = edge.data as any;
    const sourceKind: NodeKind | undefined = (sourceNode?.data as any)?.nodeKind;
    const allowedRoles = getAllowedEdgeRoles(sourceKind);

    const handleRoleChange = (e: React.ChangeEvent<HTMLSelectElement>) => {
        onChange(edge.id, { role: e.target.value });
    };

    return (
        <div style={{ fontSize: '18px', marginTop: '18px' }}>
            <h3>Edge</h3>
            <div>From: {edge.source}</div>
            <div>To: {edge.target}</div>
            {allowedRoles.length > 0 && (
                <div style={{ marginTop: '8px' }}>
                    <label>
                        Role:
                        {allowedRoles.length === 1 ? (
                            <span style={{ marginLeft: '4px' }}>{allowedRoles[0]}</span>
                        ) : (
                            <select
                                value={data?.role ?? ''}
                                onChange={handleRoleChange}
                                style={{ width: '100%', boxSizing: 'border-box', marginTop: '4px' }}
                            >
                                <option value="" style={{ fontSize: '16px' }}>Select role</option>
                                {allowedRoles.map(role => (
                                    <option key={role} value={role} style={{ fontSize: '16px' }}>
                                        {role}
                                    </option>
                                ))}
                            </select>
                        )}
                    </label>
                </div>
            )}
        </div>
    );
};

export default EdgeInspector;
