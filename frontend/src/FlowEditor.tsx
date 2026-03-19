import React from 'react';
import {
  ReactFlow,
  MiniMap,
  Controls,
  Background,
  BackgroundVariant,
  type Connection,
  type Edge as FlowEdge,
  type Node as FlowNode,
} from '@xyflow/react';
import '@xyflow/react/dist/style.css';
import type { NodeStatus } from './types';

interface FlowEditorProps {
  nodes: FlowNode[];
  edges: FlowEdge[];
  onNodesChange: any;
  onEdgesChange: any;
  onConnect: (connection: Connection) => void;
  nodeStatuses: Map<string, NodeStatus>;
  onNodeClick?: (id: string) => void;
  onEdgeClick?: (id: string) => void;
}

const FlowEditor: React.FC<FlowEditorProps> = ({
  nodes,
  edges,
  onNodesChange,
  onEdgesChange,
  onConnect,
  nodeStatuses,
  onNodeClick,
  onEdgeClick,
}) => {
  const dotsVariant: BackgroundVariant = BackgroundVariant.Dots;
    
  // Custom styling based on status
  const getStyledNodes = () => {
      return nodes.map(node => {
          const status = nodeStatuses.get(node.id);
          let style = {};
          if (status?.status === 'ACTIVE') {
              style = { border: '2px solid blue', background: '#e0f7fa' };
          } else if (status?.status === 'COMPLETED') {
              style = { border: '2px solid green', background: '#e8f5e9' };
          } else if (status?.status === 'ERROR') {
              style = { border: '2px solid red', background: '#ffebee' };
          }
          return { ...node, style: { ...node.style, ...style } };
      });
  };

  return (
    <div style={{ width: '100%', height: '100%' }}>
      <ReactFlow
        nodes={getStyledNodes()}
        edges={edges}
        onNodesChange={onNodesChange}
        onEdgesChange={onEdgesChange}
        onConnect={onConnect}
        onNodeClick={(_, node) => onNodeClick && onNodeClick(node.id)}
        onEdgeClick={(_, edge) => onEdgeClick && onEdgeClick(edge.id)}
        fitView
      >
        <Controls />
        <MiniMap />
        <Background variant={dotsVariant} gap={12} size={1} />
      </ReactFlow>
    </div>
  );
};

export default FlowEditor;
