export type NodeKind = 'start' | 'end' | 'task' | 'decision' | 'join';

export type DecisionConditionType =
    | 'LAST_TASK_CATEGORY'
    | 'LAST_TASK_RESULT_CODE'
    | 'EXTERNAL_VAR_BOOL';

export const capabilityOptions = [
    { id: 'MoveToPose', label: 'MoveToPose' },
    { id: 'PickObject', label: 'PickObject' },
    { id: 'ScanQRCode', label: 'ScanQRCode' },
];

export const decisionConditionTypes: { id: DecisionConditionType; label: string }[] = [
    { id: 'LAST_TASK_CATEGORY', label: 'Last Task Category' },
    { id: 'LAST_TASK_RESULT_CODE', label: 'Last Task Result Code' },
    { id: 'EXTERNAL_VAR_BOOL', label: 'External Variable == true' },
];

export const decisionCategoryOptions = [
    { id: 'SUCCESS', label: 'SUCCESS' },
    { id: 'RECOVERABLE_ERROR', label: 'RECOVERABLE_ERROR' },
    { id: 'FATAL_ERROR', label: 'FATAL_ERROR' },
];

export function getAllowedEdgeRoles(sourceNodeKind: NodeKind | undefined): string[] {
    if (sourceNodeKind === 'task') {
        return ['onDone', 'onError', 'onTimeout'];
    }
    if (sourceNodeKind === 'decision') {
        return ['true', 'false'];
    }
    if (sourceNodeKind === 'join') {
        return ['onReady'];
    }
    return [];
}

