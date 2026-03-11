import React, { useState, useCallback, useEffect } from 'react';
import {
    useNodesState,
    useEdgesState,
    addEdge,
    type Connection,
    type Edge,
    type Node,
} from '@xyflow/react';
import FlowEditor from './FlowEditor';
import { Client } from '@stomp/stompjs';
import axios from 'axios';
import type { NodeStatus } from './types';
import { Play, Plus, Trash2 } from 'lucide-react';
import './App.css';

const initialNodes: Node[] = [
    { id: 'start', position: { x: 100, y: 100 }, data: { label: 'Start Node' }, type: 'input' },
    { id: 'action-1', position: { x: 100, y: 200 }, data: { label: 'Action (2s)' }, type: 'default' },
    { id: 'end', position: { x: 100, y: 300 }, data: { label: 'End Node' }, type: 'output' },
];

const initialEdges: Edge[] = [
    { id: 'e1-2', source: 'start', target: 'action-1' },
    { id: 'e2-3', source: 'action-1', target: 'end' },
];

function App() {
    const [nodes, setNodes, onNodesChange] = useNodesState(initialNodes);
    const [edges, setEdges, onEdgesChange] = useEdgesState(initialEdges);
    const [nodeStatuses, setNodeStatuses] = useState<Map<string, NodeStatus>>(new Map());
    const [logs, setLogs] = useState<string[]>([]);
    const [stompClient, setStompClient] = useState<Client | null>(null);

    const onConnect = useCallback(
        (params: Connection) => setEdges((eds) => addEdge(params, eds)),
        [setEdges],
    );

    useEffect(() => {
        const client = new Client({
            brokerURL: 'ws://localhost:8080/ws',
            onConnect: () => {
                setLogs(prev => [...prev, 'Connected to WebSocket']);
                client.subscribe('/topic/status', (message) => {
                    const status: NodeStatus = JSON.parse(message.body);
                    setNodeStatuses(prev => new Map(prev).set(status.nodeId, status));
                    setLogs(prev => [...prev, `[${status.timestamp}] ${status.nodeId}: ${status.status} - ${status.message}`]);
                });
            },
            onDisconnect: () => {
                setLogs(prev => [...prev, 'Disconnected from WebSocket']);
            },
        });

        client.activate();
        setStompClient(client);

        return () => {
            client.deactivate();
        };
    }, []);

    const handleRun = async () => {
        setLogs(prev => [...prev, 'Starting execution...']);
        setNodeStatuses(new Map()); // Clear statuses
        try {
            const flowchart = {
                id: 'demo-flow-' + Date.now(),
                name: 'Demo Flow',
                nodes: nodes.map(n => ({
                    id: n.id,
                    type: n.id.includes('start') ? 'start' : n.id.includes('end') ? 'end' : 'action',
                    label: n.data.label,
                    data: n.data,
                    position: n.position
                })),
                edges: edges.map(e => ({
                    id: e.id,
                    source: e.source,
                    target: e.target
                }))
            };

            await axios.post('http://localhost:8080/api/flow/execute', flowchart);
            setLogs(prev => [...prev, 'Execution request sent.']);
        } catch (error) {
            console.error(error);
            setLogs(prev => [...prev, 'Error starting execution.']);
        }
    };

    const addNode = () => {
        const id = `node-${nodes.length + 1}`;
        const newNode: Node = {
            id,
            position: { x: Math.random() * 400, y: Math.random() * 400 },
            data: { label: `Node ${id}` },
        };
        setNodes((nds) => nds.concat(newNode));
    };

    return (
        <div style={{ display: 'flex', flexDirection: 'column', height: '100vh',width:'100vw' }}>
            <div style={{ padding: '10px', background: '#f0f0f0', borderBottom: '1px solid #ccc', display: 'flex', gap: '10px' }}>
                <button onClick={handleRun} style={{ display: 'flex', alignItems: 'center', gap: '5px', padding: '8px 16px', background: '#4caf50', color: 'white', border: 'none', borderRadius: '4px', cursor: 'pointer' }}>
                    <Play size={16} /> Run
                </button>
                <button onClick={addNode} style={{ display: 'flex', alignItems: 'center', gap: '5px', padding: '8px 16px', background: '#2196f3', color: 'white', border: 'none', borderRadius: '4px', cursor: 'pointer' }}>
                    <Plus size={16} /> Add Node
                </button>
            </div>
            
            <div style={{ flex: 1, display: 'flex' }}>
                <div style={{ flex: 3, position: 'relative' }}>
                    <FlowEditor
                        nodes={nodes}
                        edges={edges}
                        onNodesChange={onNodesChange}
                        onEdgesChange={onEdgesChange}
                        onConnect={onConnect}
                        nodeStatuses={nodeStatuses}
                    />
                </div>
                <div style={{ flex: 1, borderLeft: '1px solid #ccc', padding: '10px', overflowY: 'auto', background: '#fafafa' }}>
                    <h3>Logs</h3>
                    {logs.map((log, i) => (
                        <div key={i} style={{ fontSize: '12px', marginBottom: '5px', fontFamily: 'monospace' }}>
                            {log}
                        </div>
                    ))}
                </div>
            </div>
        </div>
    );
}

export default App;
