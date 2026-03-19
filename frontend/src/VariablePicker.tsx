import React from 'react';
import type { Node } from '@xyflow/react';
import type { ExternalVarDef, LogicalInternalVarDef, VariableRef, VariableSource } from './types';

type Props = {
    value?: VariableRef;
    externalVars: ExternalVarDef[];
    internalVars: LogicalInternalVarDef[];
    taskNodes: Node[];
    allowedSources?: VariableSource[];
    onChange: (next: VariableRef) => void;
};

const defaultSources: VariableSource[] = ['external', 'internal', 'taskResult', 'constant'];

const VariablePicker: React.FC<Props> = ({
    value,
    externalVars,
    internalVars,
    taskNodes,
    allowedSources,
    onChange,
}) => {
    const current: VariableRef = value ?? { source: 'external', name: '' };
    const sources = allowedSources && allowedSources.length > 0 ? allowedSources : defaultSources;

    const setSource = (source: VariableSource) => {
        if (source === 'external') {
            onChange({ source, name: '' });
            return;
        }
        if (source === 'internal') {
            onChange({ source, name: '' });
            return;
        }
        if (source === 'taskResult') {
            onChange({ source, name: '', field: 'category' });
            return;
        }
        onChange({ source, name: '', constantValue: '' });
    };

    const setName = (name: string) => {
        onChange({ ...current, name });
    };

    const setField = (field: 'resultCode' | 'category') => {
        onChange({ ...current, field });
    };

    const setConstantValue = (constantValue: any) => {
        onChange({ ...current, constantValue });
    };

    return (
        <div style={{ display: 'flex', gap: '8px', alignItems: 'flex-end' }}>
            <label style={{ width: '120px' }}>
                Source:
                <select
                    value={current.source}
                    onChange={e => setSource(e.target.value as VariableSource)}
                    style={{ width: '100%', boxSizing: 'border-box', marginTop: '4px' }}
                >
                    {sources.map(s => (
                        <option key={s} value={s}>
                            {s}
                        </option>
                    ))}
                </select>
            </label>

            {current.source === 'external' && (
                <label style={{ flex: 1 }}>
                    External:
                    <select
                        value={current.name}
                        onChange={e => setName(e.target.value)}
                        style={{ width: '100%', boxSizing: 'border-box', marginTop: '4px' }}
                    >
                        <option value="">Select external var</option>
                        {externalVars.map(v => (
                            <option key={v.name} value={v.name}>
                                {v.name}
                            </option>
                        ))}
                    </select>
                </label>
            )}

            {current.source === 'internal' && (
                <label style={{ flex: 1 }}>
                    Internal:
                    <select
                        value={current.name}
                        onChange={e => setName(e.target.value)}
                        style={{ width: '100%', boxSizing: 'border-box', marginTop: '4px' }}
                    >
                        <option value="">Select internal var</option>
                        {internalVars.map(v => (
                            <option key={v.name} value={v.name}>
                                {v.name}
                            </option>
                        ))}
                    </select>
                </label>
            )}

            {current.source === 'taskResult' && (
                <>
                    <label style={{ flex: 1 }}>
                        Task:
                        <select
                            value={current.name}
                            onChange={e => setName(e.target.value)}
                            style={{ width: '100%', boxSizing: 'border-box', marginTop: '4px' }}
                        >
                            <option value="">Select task</option>
                            {taskNodes.map(t => (
                                <option key={t.id} value={t.id}>
                                    {(t.data as any)?.label || t.id}
                                </option>
                            ))}
                        </select>
                    </label>
                    <label style={{ width: '140px' }}>
                        Field:
                        <select
                            value={current.field ?? 'category'}
                            onChange={e => setField(e.target.value as 'resultCode' | 'category')}
                            style={{ width: '100%', boxSizing: 'border-box', marginTop: '4px' }}
                        >
                            <option value="category">category</option>
                            <option value="resultCode">resultCode</option>
                        </select>
                    </label>
                </>
            )}

            {current.source === 'constant' && (
                <label style={{ flex: 1 }}>
                    Constant:
                    <input
                        type="text"
                        value={current.constantValue ?? ''}
                        onChange={e => setConstantValue(e.target.value)}
                        style={{ width: '100%', boxSizing: 'border-box', marginTop: '4px' }}
                    />
                </label>
            )}
        </div>
    );
};

export default VariablePicker;

