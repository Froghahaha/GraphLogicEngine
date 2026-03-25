import { useState, useCallback, useEffect } from 'react';
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
import type { ExternalVarDef, LogicalInternalVarDef, NodeStatus, CapabilityMetadata } from './types';
import NodeInspector from './NodeInspector';
import EdgeInspector from './EdgeInspector';
import VariablePanel from './VariablePanel';
import { Play, Plus } from 'lucide-react';
import './App.css';

type LogLevel = 'info' | 'warn' | 'error' | 'debug';

type LogSource = 'ui' | 'flow' | 'ws' | 'backend';

type LogEvent = {
    time: number;
    level: LogLevel;
    source: LogSource;
    action: string;
    payload?: any;
    message?: string;
};

const initialNodes: Node[] = [
    { id: 'start', position: { x: 100, y: 100 }, data: { label: 'Start Node', nodeKind: 'start' }, type: 'input' },
    { id: 'task-1', position: { x: 100, y: 200 }, data: { label: 'Task (2s)', nodeKind: 'task' }, type: 'default' },
    { id: 'end', position: { x: 100, y: 300 }, data: { label: 'End Node', nodeKind: 'end' }, type: 'output' },
];

const initialEdges: Edge[] = [
    { id: 'e1-2', source: 'start', target: 'task-1' },
    { id: 'e2-3', source: 'task-1', target: 'end' },
];

function App() {
    const [nodes, setNodes, onNodesChange] = useNodesState(initialNodes);
    const [edges, setEdges, onEdgesChange] = useEdgesState(initialEdges);
    const [nodeStatuses, setNodeStatuses] = useState<Map<string, NodeStatus>>(new Map());
    const [logs, setLogs] = useState<LogEvent[]>([]);
    const [, setStompClient] = useState<Client | null>(null);
    const [selectedNodeId, setSelectedNodeId] = useState<string | null>(null);
    const [selectedEdgeId, setSelectedEdgeId] = useState<string | null>(null);
    const [externalVars, setExternalVars] = useState<ExternalVarDef[]>([]);
    const [internalVars, setInternalVars] = useState<LogicalInternalVarDef[]>([]);
    const [capabilities, setCapabilities] = useState<CapabilityMetadata[]>([]);

    const pushLog = useCallback((event: Omit<LogEvent, 'time'>) => {
        setLogs(prev => [...prev, { ...event, time: Date.now() }]);
    }, []);

    const onConnect = useCallback(
        (params: Connection) => {
            setEdges((eds) => addEdge(params, eds));
            pushLog({
                level: 'info',
                source: 'ui',
                action: 'edge.add',
                payload: { source: params.source, target: params.target },
            });
        },
        [setEdges, pushLog],
    );

    const handleNodeClick = useCallback((id: string) => {
        setSelectedNodeId(id);
        setSelectedEdgeId(null);
    }, []);

    const handleEdgeClick = useCallback((id: string) => {
        setSelectedEdgeId(id);
        setSelectedNodeId(null);
    }, []);

    useEffect(() => {
        const client = new Client({
            brokerURL: 'ws://localhost:8080/ws',
            onConnect: () => {
                pushLog({
                    level: 'info',
                    source: 'ws',
                    action: 'ws.connected',
                    message: 'Connected to WebSocket',
                });
                client.subscribe('/topic/status', (message) => {
                    const status: NodeStatus = JSON.parse(message.body);
                    setNodeStatuses(prev => new Map(prev).set(status.nodeId, status));
                    pushLog({
                        level: 'debug',
                        source: 'ws',
                        action: 'ws.nodeStatus',
                        payload: status,
                    });
                });
            },
            onDisconnect: () => {
                pushLog({
                    level: 'info',
                    source: 'ws',
                    action: 'ws.disconnected',
                    message: 'Disconnected from WebSocket',
                });
            },
        });

        client.activate();
        setStompClient(client);

        return () => {
            client.deactivate();
        };
    }, [pushLog]);

    useEffect(() => {
        axios
            .get<ExternalVarDef[]>('http://localhost:8080/api/schema/external-vars')
            .then(res => setExternalVars(res.data))
            .catch(err =>
                pushLog({
                    level: 'error',
                    source: 'backend',
                    action: 'schema.externalVars.error',
                    message: 'Failed to load external vars',
                    payload: { error: String(err) },
                }),
            );

        axios
            .get<CapabilityMetadata[]>('http://localhost:8080/api/schema/capabilities')
            .then(res => setCapabilities(res.data))
            .catch(err =>
                pushLog({
                    level: 'error',
                    source: 'backend',
                    action: 'schema.capabilities.error',
                    message: 'Failed to load capabilities',
                    payload: { error: String(err) },
                }),
            );
    }, [pushLog]);

    const handleRun = async () => {
        pushLog({
            level: 'info',
            source: 'flow',
            action: 'flow.run.start',
            message: 'Starting execution',
            payload: { nodeCount: nodes.length, edgeCount: edges.length },
        });
        setNodeStatuses(new Map());
        try {
            const flowchart = {
                id: 'demo-flow-' + Date.now(),
                name: 'Demo Flow',
                nodes: nodes.map(n => {
                    const data: any = n.data || {};
                    const nodeKind = data.nodeKind;
                    let type = nodeKind;
                    if (!type) {
                        if (n.id.includes('start')) {
                            type = 'start';
                        } else if (n.id.includes('end')) {
                            type = 'end';
                        } else {
                            type = 'action';
                        }
                    }
                    return {
                        id: n.id,
                        type,
                        label: data.label,
                        data,
                        position: n.position
                    };
                }),
                edges: edges.map(e => ({
                    id: e.id,
                    source: e.source,
                    target: e.target,
                    data: e.data
                })),
                internalVars
            };

            await axios.post('http://localhost:8080/api/flow/execute', flowchart);
            pushLog({
                level: 'info',
                source: 'backend',
                action: 'flow.run.request.ok',
                message: 'Execution request sent',
                payload: { flowId: flowchart.id },
            });
        } catch (error) {
            console.error(error);
            pushLog({
                level: 'error',
                source: 'backend',
                action: 'flow.run.request.error',
                message: 'Error starting execution',
                payload: { error: String(error) },
            });
        }
    };

    const addNodeOfKind = (kind: 'start' | 'end' | 'task' | 'decision' | 'join') => {
        const index = nodes.length + 1;
        const id = `${kind}-${index}`;
        let type: Node['type'] = 'default';
        if (kind === 'start') {
            type = 'input';
        } else if (kind === 'end') {
            type = 'output';
        }
        const label =
            kind === 'start'
                ? 'Start'
                : kind === 'end'
                ? 'End'
                : kind === 'task'
                ? 'Task'
                : kind === 'decision'
                ? 'Decision'
                : 'Join';
        const newNode: Node = {
            id,
            position: { x: 100 + Math.random() * 300, y: 100 + Math.random() * 300 },
            data: { label, nodeKind: kind },
            type,
        };
        setNodes((nds) => nds.concat(newNode));
        pushLog({
            level: 'info',
            source: 'ui',
            action: 'node.add',
            payload: { id, kind },
        });
    };

    const handleNodeDataChange = (id: string, patch: Record<string, any>) => {
        setNodes((nds) =>
            nds.map((n) =>
                n.id === id
                    ? {
                          ...n,
                          data: { ...(n.data as any), ...patch },
                      }
                    : n,
            ),
        );
        pushLog({
            level: 'debug',
            source: 'ui',
            action: 'node.update',
            payload: { id, patch },
        });
    };

    const handleEdgeDataChange = (id: string, patch: Record<string, any>) => {
        setEdges((eds) =>
            eds.map((e) =>
                e.id === id
                    ? {
                          ...e,
                          data: { ...(e.data as any), ...patch },
                      }
                    : e,
            ),
        );
        pushLog({
            level: 'debug',
            source: 'ui',
            action: 'edge.update',
            payload: { id, patch },
        });
    };

    const deleteNodeById = (id: string) => {
        setNodes(prevNodes => prevNodes.filter(n => n.id !== id));
        setEdges(prevEdges => prevEdges.filter(e => e.source !== id && e.target !== id));
        setSelectedNodeId(null);
        pushLog({
            level: 'info',
            source: 'ui',
            action: 'node.delete',
            payload: { id },
        });
    };

    const deleteEdgeById = (id: string) => {
        let removedEdge: Edge | undefined;
        setEdges(prevEdges => {
            const remaining = prevEdges.filter(e => {
                if (e.id === id) {
                    removedEdge = e;
                    return false;
                }
                return true;
            });
            return remaining;
        });
        setSelectedEdgeId(null);
        if (removedEdge) {
            pushLog({
                level: 'info',
                source: 'ui',
                action: 'edge.delete',
                payload: {
                    id: removedEdge.id,
                    source: removedEdge.source,
                    target: removedEdge.target,
                },
            });
        }
    };

    const handleDeleteSelection = () => {
        if (selectedNodeId) {
            deleteNodeById(selectedNodeId);
        } else if (selectedEdgeId) {
            deleteEdgeById(selectedEdgeId);
        } else {
            pushLog({
                level: 'debug',
                source: 'ui',
                action: 'delete.noSelection',
                message: 'No node or edge selected',
            });
        }
    };

    const selectedNode = selectedNodeId ? nodes.find((n) => n.id === selectedNodeId) || null : null;
    const selectedEdge = selectedEdgeId ? edges.find((e) => e.id === selectedEdgeId) || null : null;
    const allTaskNodes = nodes.filter((n) => (n.data as any)?.nodeKind === 'task');

    return (
        <div style={{ display: 'flex', flexDirection: 'column', height: '100vh',width:'100vw' }}>
            <div style={{ padding: '10px', background: '#f0f0f0', borderBottom: '1px solid #ccc', display: 'flex', gap: '10px' }}>
                <button onClick={handleRun} style={{ display: 'flex', alignItems: 'center', gap: '5px', padding: '8px 16px', background: '#4caf50', color: 'white', border: 'none', borderRadius: '4px', cursor: 'pointer' }}>
                    <Play size={16} /> Run
                </button>
                <button onClick={() => addNodeOfKind('start')} style={{ display: 'flex', alignItems: 'center', gap: '5px', padding: '8px 16px', background: '#1976d2', color: 'white', border: 'none', borderRadius: '4px', cursor: 'pointer' }}>
                    <Plus size={16} /> Start
                </button>
                <button onClick={() => addNodeOfKind('task')} style={{ display: 'flex', alignItems: 'center', gap: '5px', padding: '8px 16px', background: '#2196f3', color: 'white', border: 'none', borderRadius: '4px', cursor: 'pointer' }}>
                    <Plus size={16} /> Task
                </button>
                <button onClick={() => addNodeOfKind('decision')} style={{ display: 'flex', alignItems: 'center', gap: '5px', padding: '8px 16px', background: '#ff9800', color: 'white', border: 'none', borderRadius: '4px', cursor: 'pointer' }}>
                    <Plus size={16} /> Decision
                </button>
                <button onClick={() => addNodeOfKind('join')} style={{ display: 'flex', alignItems: 'center', gap: '5px', padding: '8px 16px', background: '#9c27b0', color: 'white', border: 'none', borderRadius: '4px', cursor: 'pointer' }}>
                    <Plus size={16} /> Join
                </button>
                <button onClick={() => addNodeOfKind('end')} style={{ display: 'flex', alignItems: 'center', gap: '5px', padding: '8px 16px', background: '#607d8b', color: 'white', border: 'none', borderRadius: '4px', cursor: 'pointer' }}>
                    <Plus size={16} /> End
                </button>
                <button onClick={handleDeleteSelection} style={{ display: 'flex', alignItems: 'center', gap: '5px', padding: '8px 16px', background: '#f44336', color: 'white', border: 'none', borderRadius: '4px', cursor: 'pointer' }}>
                    Delete
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
                        onNodeClick={handleNodeClick}
                        onEdgeClick={handleEdgeClick}
                    />
                </div>
                <div style={{ flex: 1, borderLeft: '1px solid #ccc', padding: '10px', overflowY: 'auto', background: '#fafafa', display: 'flex', flexDirection: 'column', gap: '12px' }}>
                    <NodeInspector
                        node={selectedNode}
                        allNodes={nodes}
                        allTaskNodes={allTaskNodes}
                        edges={edges}
                        selectedEdgeId={selectedEdgeId}
                        onSelectEdge={setSelectedEdgeId}
                        externalVars={externalVars}
                        internalVars={internalVars}
                        capabilities={capabilities}
                        onChange={handleNodeDataChange}
                    />
                    <EdgeInspector
                        edge={selectedEdge}
                        sourceNode={
                            selectedEdge
                                ? nodes.find((n) => n.id === selectedEdge.source) || null
                                : null
                        }
                        onChange={handleEdgeDataChange}
                    />
                    <VariablePanel vars={internalVars} onChange={setInternalVars} />
                    <div>
                        <h3>Logs</h3>
                        {logs.map((log, i) => (
                            <div key={i} style={{ fontSize: '12px', marginBottom: '5px', fontFamily: 'monospace' }}>
                                [{new Date(log.time).toLocaleTimeString()}] [{log.level}] [{log.source}] {log.action}
                                {log.message ? ` - ${log.message}` : ''}
                            </div>
                        ))}
                    </div>
                </div>
            </div>
        </div>
    );
}

export default App;
