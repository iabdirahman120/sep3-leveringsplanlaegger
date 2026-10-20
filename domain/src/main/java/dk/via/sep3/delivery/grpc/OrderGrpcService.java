package dk.via.sep3.delivery.grpc;

import dk.via.sep3.delivery.order.DeliveryOrder;
import dk.via.sep3.delivery.order.InvalidTransitionException;
import dk.via.sep3.delivery.order.OrderEventEntity;
import dk.via.sep3.delivery.order.OrderNotFoundException;
import dk.via.sep3.delivery.order.OrderService;
import dk.via.sep3.delivery.order.StaleVersionException;
import dk.via.sep3.delivery.order.Status;
import io.grpc.Status.Code;
import io.grpc.stub.StreamObserver;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.stereotype.Component;

import java.util.UUID;
import java.util.function.Supplier;

/**
 * Oversætter mellem gRPC-beskederne fra proto/delivery.proto og domænet.
 * Ingen forretningsregler her, kun mapning og fejlkoder.
 */
@Component
public class OrderGrpcService extends OrderServiceGrpc.OrderServiceImplBase {

    private final OrderService orders;

    public OrderGrpcService(OrderService orders) {
        this.orders = orders;
    }

    @Override
    public void listOrders(ListOrdersRequest request, StreamObserver<ListOrdersResponse> responseObserver) {
        respond(responseObserver, () -> {
            ListOrdersResponse.Builder response = ListOrdersResponse.newBuilder();
            orders.listOrders().forEach(o -> response.addOrders(toProto(o)));
            return response.build();
        });
    }

    @Override
    public void getOrder(GetOrderRequest request, StreamObserver<GetOrderResponse> responseObserver) {
        respond(responseObserver, () -> {
            UUID id = parseId(request.getOrderId());
            GetOrderResponse.Builder response = GetOrderResponse.newBuilder().setOrder(toProto(orders.getOrder(id)));
            orders.timeline(id).forEach(e -> response.addTimeline(toProto(e)));
            return response.build();
        });
    }

    @Override
    public void getAllowedTransitions(GetAllowedTransitionsRequest request,
                                      StreamObserver<GetAllowedTransitionsResponse> responseObserver) {
        respond(responseObserver, () -> {
            GetAllowedTransitionsResponse.Builder response = GetAllowedTransitionsResponse.newBuilder();
            orders.allowedTransitions(parseId(request.getOrderId()))
                    .forEach(s -> response.addAllowed(toProto(s)));
            return response.build();
        });
    }

    @Override
    public void transitionOrder(TransitionOrderRequest request, StreamObserver<Order> responseObserver) {
        respond(responseObserver, () -> {
            if (request.getToStatus() == OrderStatus.ORDER_STATUS_UNSPECIFIED
                    || request.getToStatus() == OrderStatus.UNRECOGNIZED) {
                throw io.grpc.Status.INVALID_ARGUMENT.withDescription("to_status mangler").asRuntimeException();
            }
            DeliveryOrder updated = orders.transition(parseId(request.getOrderId()),
                    fromProto(request.getToStatus()), request.getExpectedVersion(), request.getNote());
            return toProto(updated);
        });
    }

    // Domænefejl bliver til gRPC-statuskoder, som C# oversætter videre til HTTP.
    private static <T> void respond(StreamObserver<T> observer, Supplier<T> call) {
        try {
            observer.onNext(call.get());
            observer.onCompleted();
        } catch (OrderNotFoundException e) {
            observer.onError(error(Code.NOT_FOUND, e.getMessage()));
        } catch (InvalidTransitionException e) {
            observer.onError(error(Code.FAILED_PRECONDITION, e.getMessage()));
        } catch (StaleVersionException e) {
            observer.onError(error(Code.ABORTED, e.getMessage()));
        } catch (ObjectOptimisticLockingFailureException e) {
            observer.onError(error(Code.ABORTED, "Ordren blev ændret samtidig af en anden. Genindlæs"));
        } catch (io.grpc.StatusRuntimeException e) {
            observer.onError(e);
        } catch (RuntimeException e) {
            observer.onError(error(Code.INTERNAL, "Uventet fejl i domæneserveren"));
        }
    }

    private static io.grpc.StatusRuntimeException error(Code code, String message) {
        return io.grpc.Status.fromCode(code).withDescription(message).asRuntimeException();
    }

    private static UUID parseId(String id) {
        try {
            return UUID.fromString(id);
        } catch (IllegalArgumentException e) {
            throw io.grpc.Status.INVALID_ARGUMENT.withDescription("Ugyldigt ordre-id").asRuntimeException();
        }
    }

    private static Order toProto(DeliveryOrder o) {
        return Order.newBuilder()
                .setId(o.getId().toString())
                .setStatus(toProto(o.getStatus()))
                .setWarehouseId(o.getWarehouseId())
                .setCustomerId(o.getCustomerName())
                .addItems(OrderItem.newBuilder().setProductName(o.getProductName()).setQuantity(1))
                .setDeliveryWindowStart(o.getDeliveryWindowStart() == null ? "" : o.getDeliveryWindowStart().toString())
                .setDeliveryWindowEnd(o.getDeliveryWindowEnd() == null ? "" : o.getDeliveryWindowEnd().toString())
                .setCreatedAt(o.getCreatedAt().toString())
                .setVersion(o.getVersion())
                .build();
    }

    private static OrderEvent toProto(OrderEventEntity e) {
        return OrderEvent.newBuilder()
                .setId(e.getId().toString())
                .setOrderId(e.getOrderId().toString())
                .setFromStatus(toProto(e.getFromStatus()))
                .setToStatus(toProto(e.getToStatus()))
                .setNote(e.getNote() == null ? "" : e.getNote())
                .setOccurredAt(e.getOccurredAt().toString())
                .build();
    }

    static OrderStatus toProto(Status s) {
        return OrderStatus.valueOf("ORDER_STATUS_" + s.name());
    }

    static Status fromProto(OrderStatus s) {
        return Status.valueOf(s.name().substring("ORDER_STATUS_".length()));
    }
}
