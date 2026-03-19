import React from 'react';
import type { Node } from '@xyflow/react';
import type { ExternalVarDef } from './types';
import {
    capabilityOptions,
} from './nodeSchemas';
import type { LogicalInternalVarDef, DecisionCondition } from './types';
import VariablePicker from './VariablePicker';
import type { Edge } from '@xyflow/react';

type Props = {
    node: Node | null;
    allTaskNodes: Node[];
    edges: Edge[];
    selectedEdgeId: string | null;
    onSelectEdge: (id: string | null) => void;
    externalVars: ExternalVarDef[];
    internalVars: LogicalInternalVarDef[];
    onChange: (id: string, patch: Record<string, any>) => void;
};

const NodeInspector: React.FC<Props> = ({ node, allTaskNodes, edges, selectedEdgeId, onSelectEdge, externalVars, internalVars, onChange }) => {
    if (!node) {
        return (
            <div style={{ fontSize: '12px' }}>
                No node selected
            </div>
        );
    }

    const data = node.data as any;
    const nodeKind = data?.nodeKind;

    const handleFieldChange = (patch: Record<string, any>) => {
        onChange(node.id, patch);
    };

    const handleLabelChange = (e: React.ChangeEvent<HTMLInputElement>) => {
        handleFieldChange({ label: e.target.value });
    };

    const handleCapabilityIdChange = (e: React.ChangeEvent<HTMLSelectElement>) => {
        handleFieldChange({ capabilityId: e.target.value });
    };

    const handleTimeoutChange = (e: React.ChangeEvent<HTMLInputElement>) => {
        const value = e.target.value;
        handleFieldChange({ timeoutMs: value === '' ? undefined : Number(value) });
    };

    const decisionCondition: DecisionCondition | undefined = data?.decision?.condition;
    const leftRef = decisionCondition?.left;
    const opValue = decisionCondition?.op ?? 'EQ';
    const rightLiteral = decisionCondition && 'literal' in (decisionCondition.right as any) ? (decisionCondition.right as any).literal : '';

    const setDecisionCondition = (next: Partial<DecisionCondition>) => {
        const base: DecisionCondition = decisionCondition ?? {
            left: { source: 'external', name: '' },
            op: 'EQ',
            right: { literal: '' },
        };
        const merged: DecisionCondition = { ...base, ...next };
        handleFieldChange({ decision: { ...(data?.decision || {}), condition: merged } });
    };
 
    const handleOpChange = (e: React.ChangeEvent<HTMLSelectElement>) => {
        setDecisionCondition({ op: e.target.value as DecisionCondition['op'] });
    };

    const handleRightLiteralChange = (e: React.ChangeEvent<HTMLInputElement>) => {
        setDecisionCondition({ right: { literal: e.target.value } });
    };

    const handleModeChange = (e: React.ChangeEvent<HTMLSelectElement>) => {
        handleFieldChange({ mode: e.target.value });
    };

    const handleTaskCheckboxChange = (taskId: string, checked: boolean) => {
        const current: string[] = Array.isArray(data?.taskIds) ? data.taskIds : [];
        const next = checked ? Array.from(new Set(current.concat(taskId))) : current.filter(id => id !== taskId);
        handleFieldChange({ taskIds: next });
    };

    const outgoingEdges = edges.filter(e => e.source === node.id);

    return (
        <div style={{ fontSize: '12px' }}>
            <h3>Node</h3>
            <div>ID: {node.id}</div>
            <div>Kind: {nodeKind || 'unknown'}</div>
            <div style={{ marginTop: '8px' }}>
                <label>
                    Label:
                    <input
                        type="text"
                        value={data?.label ?? ''}
                        onChange={handleLabelChange}
                        style={{ width: '100%', boxSizing: 'border-box', marginTop: '4px' }}
                    />
                </label>
            </div>
            {nodeKind === 'task' && (
                <div style={{ marginTop: '8px' }}>
                    <label>
                        Capability ID:
                        <select
                            value={data?.capabilityId ?? ''}
                            onChange={handleCapabilityIdChange}
                            style={{ width: '100%', boxSizing: 'border-box', marginTop: '4px' }}
                        >
                            <option value="">Select capability</option>
                            {capabilityOptions.map(opt => (
                                <option key={opt.id} value={opt.id}>
                                    {opt.label}
                                </option>
                            ))}
                        </select>
                    </label>
                    <label style={{ display: 'block', marginTop: '8px' }}>
                        Timeout (ms):
                        <input
                            type="number"
                            value={data?.timeoutMs ?? ''}
                            onChange={handleTimeoutChange}
                            style={{ width: '100%', boxSizing: 'border-box', marginTop: '4px' }}
                        />
                    </label>
                </div>
            )}
            {nodeKind === 'decision' && (
                <div style={{ marginTop: '8px' }}>
                    <div>Condition</div>
                    <div style={{ marginTop: '6px' }}>
                        <VariablePicker
                            value={leftRef}
                            externalVars={externalVars}
                            internalVars={internalVars}
                            taskNodes={allTaskNodes}
                            allowedSources={['external', 'internal', 'taskResult']}
                            onChange={(next) => setDecisionCondition({ left: next })}
                        />
                    </div>
                    <div style={{ display: 'flex', gap: '8px', marginTop: '8px' }}>
                        <label style={{ width: '120px' }}>
                            Op:
                            <select
                                value={opValue}
                                onChange={handleOpChange}
                                style={{ width: '100%', boxSizing: 'border-box', marginTop: '4px' }}
                            >
                                <option value="EQ">==</option>
                                <option value="NEQ">!=</option>
                            </select>
                        </label>
                        <label style={{ flex: 1 }}>
                            Right (literal):
                            <input
                                type="text"
                                value={rightLiteral}
                                onChange={handleRightLiteralChange}
                                style={{ width: '100%', boxSizing: 'border-box', marginTop: '4px' }}
                            />
                        </label>
                    </div>
                </div>
            )}
            {nodeKind === 'join' && (
                <div style={{ marginTop: '8px' }}>
                    <label>
                        Mode:
                        <select
                            value={data?.mode ?? 'WaitAllDone'}
                            onChange={handleModeChange}
                            style={{ width: '100%', boxSizing: 'border-box', marginTop: '4px' }}
                        >
                            <option value="WaitAllDone">WaitAllDone</option>
                            <option value="WaitAnyDone">WaitAnyDone</option>
                            <option value="WaitAllFinished">WaitAllFinished</option>
                        </select>
                    </label>
                    <div style={{ marginTop: '8px' }}>
                        <div>Tasks:</div>
                        {allTaskNodes.map(taskNode => {
                            const checked = Array.isArray(data?.taskIds) && data.taskIds.includes(taskNode.id);
                            const taskLabel = (taskNode.data as any)?.label || taskNode.id;
                            return (
                                <label key={taskNode.id} style={{ display: 'block' }}>
                                    <input
                                        type="checkbox"
                                        checked={checked}
                                        onChange={e => handleTaskCheckboxChange(taskNode.id, e.target.checked)}
                                    />
                                    <span>{taskLabel}</span>
                                </label>
                            );
                        })}
                    </div>
                </div>
            )}
            <div style={{ marginTop: '12px' }}>
                <h3 style={{ marginBottom: '6px' }}>Outgoing</h3>
                {outgoingEdges.length === 0 && <div>No outgoing edges</div>}
                {outgoingEdges.map(e => {
                    const active = selectedEdgeId === e.id;
                    const role = (e.data as any)?.role;
                    return (
                        <div
                            key={e.id}
                            style={{
                                display: 'flex',
                                alignItems: 'center',
                                justifyContent: 'space-between',
                                border: '1px solid #ddd',
                                padding: '6px',
                                borderRadius: '6px',
                                background: active ? '#e3f2fd' : '#fff',
                                cursor: 'pointer',
                            }}
                            onClick={() => onSelectEdge(e.id)}
                        >
                            <div style={{ overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap' }}>
                                {role ? role : '(unset)'} → {e.target}
                            </div>
                            <div style={{ fontSize: '11px', color: '#666' }}>Edit</div>
                        </div>
                    );
                })}
            </div>
        </div>
    );
};

export default NodeInspector;
