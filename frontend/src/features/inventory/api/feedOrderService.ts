import {
    useGetAllOrders,
    useCreateOrder,
    useGetOrderMessage,
    useConfirmOrder,
    useReceiveOrder,
    useCancelOrder
} from '../../../api/manual/hooks';

export const feedOrderService = {
    useGetAll: useGetAllOrders,
    useCreate: useCreateOrder,
    useGetOrderMessage,
    useConfirm: useConfirmOrder,
    useReceive: useReceiveOrder,
    useCancel: useCancelOrder,
};
