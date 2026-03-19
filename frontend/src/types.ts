export interface NodeData {
    label: string;
    nodeKind?: 'start' | 'end' | 'task' | 'decision' | 'join';
    capabilityId?: string;
    timeoutMs?: number;
    condition?: string;
    mode?: 'WaitAllDone' | 'WaitAnyDone' | 'WaitAllFinished';
    taskIds?: string[];
    decision?: {
        condition?: DecisionCondition;
    };
    [key: string]: any;
}

export interface ExternalVarDef {
    name: string;
    type: string;
    category: string;
    description: string;
}

export interface InternalVarDef {
    name: string;
    type: string;
    persistent: boolean;
    description: string;
}

export interface Node {
    id: string;
    type: string;
    position: { x: number; y: number };
    data: NodeData;
}

export interface Edge {
    id: string;
    source: string;
    target: string;
    data?: {
        role?: string;
        [key: string]: any;
    };
}

export interface Flowchart {
    id: string;
    name: string;
    nodes: Node[];
    edges: Edge[];
    internalVars?: LogicalInternalVarDef[];
}

export interface NodeStatus {
    nodeId: string;
    status: 'ACTIVE' | 'COMPLETED' | 'ERROR';
    message: string;
    timestamp: number;
}

export interface LogicalInternalVarDef {
    name: string;
    type: 'bool' | 'int' | 'float' | 'string';
    initial?: string;
    description?: string;
}

export type VariableSource = 'external' | 'internal' | 'taskResult' | 'constant';

export interface VariableRef {
    source: VariableSource;
    name: string;
    field?: 'resultCode' | 'category';
    constantValue?: any;
}

export type ComparisonOp = 'EQ' | 'NEQ';

export interface DecisionCondition {
    left: VariableRef;
    op: ComparisonOp;
    right: VariableRef | { literal: any };
}
