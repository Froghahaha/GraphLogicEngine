export interface NodeData {
    label: string;
    [key: string]: any;
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
}

export interface Flowchart {
    id: string;
    name: string;
    nodes: Node[];
    edges: Edge[];
}

export interface NodeStatus {
    nodeId: string;
    status: 'ACTIVE' | 'COMPLETED' | 'ERROR';
    message: string;
    timestamp: number;
}
