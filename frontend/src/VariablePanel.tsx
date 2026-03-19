import React from 'react';
import type { LogicalInternalVarDef } from './types';

type Props = {
    vars: LogicalInternalVarDef[];
    onChange: (next: LogicalInternalVarDef[]) => void;
};

const VariablePanel: React.FC<Props> = ({ vars, onChange }) => {
    const addVar = () => {
        const index = vars.length + 1;
        const name = `var_${index}`;
        onChange(vars.concat({ name, type: 'bool', initial: 'false', description: '' }));
    };

    const updateVar = (i: number, patch: Partial<LogicalInternalVarDef>) => {
        const next = vars.map((v, idx) => (idx === i ? { ...v, ...patch } : v));
        onChange(next);
    };

    const removeVar = (i: number) => {
        onChange(vars.filter((_, idx) => idx !== i));
    };

    return (
        <div style={{ fontSize: '12px' }}>
            <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between' }}>
                <h3 style={{ margin: 0 }}>Internal Vars</h3>
                <button onClick={addVar} style={{ padding: '6px 10px' }}>
                    Add Variable
                </button>
            </div>
            <div style={{ marginTop: '8px', display: 'flex', flexDirection: 'column', gap: '8px' }}>
                {vars.length === 0 && <div>No internal variables</div>}
                {vars.map((v, i) => (
                    <div key={`${v.name}-${i}`} style={{ border: '1px solid #ddd', padding: '8px', borderRadius: '6px', background: '#fff' }}>
                        <div style={{ display: 'flex', gap: '8px' }}>
                            <label style={{ flex: 1 }}>
                                Name:
                                <input
                                    type="text"
                                    value={v.name}
                                    onChange={e => updateVar(i, { name: e.target.value })}
                                    style={{ width: '100%', boxSizing: 'border-box', marginTop: '4px' }}
                                />
                            </label>
                            <label style={{ width: '140px' }}>
                                Type:
                                <select
                                    value={v.type}
                                    onChange={e => updateVar(i, { type: e.target.value as LogicalInternalVarDef['type'] })}
                                    style={{ width: '100%', boxSizing: 'border-box', marginTop: '4px' }}
                                >
                                    <option value="bool">bool</option>
                                    <option value="int">int</option>
                                    <option value="float">float</option>
                                    <option value="string">string</option>
                                </select>
                            </label>
                        </div>
                        <div style={{ display: 'flex', gap: '8px', marginTop: '8px' }}>
                            <label style={{ flex: 1 }}>
                                Initial:
                                <input
                                    type="text"
                                    value={v.initial ?? ''}
                                    onChange={e => updateVar(i, { initial: e.target.value })}
                                    style={{ width: '100%', boxSizing: 'border-box', marginTop: '4px' }}
                                />
                            </label>
                            <label style={{ flex: 2 }}>
                                Description:
                                <input
                                    type="text"
                                    value={v.description ?? ''}
                                    onChange={e => updateVar(i, { description: e.target.value })}
                                    style={{ width: '100%', boxSizing: 'border-box', marginTop: '4px' }}
                                />
                            </label>
                            <div style={{ display: 'flex', alignItems: 'flex-end' }}>
                                <button onClick={() => removeVar(i)} style={{ padding: '6px 10px', background: '#f44336', color: '#fff' }}>
                                    Delete
                                </button>
                            </div>
                        </div>
                    </div>
                ))}
            </div>
        </div>
    );
};

export default VariablePanel;

