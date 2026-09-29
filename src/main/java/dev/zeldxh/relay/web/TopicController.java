package dev.zeldxh.relay.web;

import dev.zeldxh.relay.audit.AuditLogService;
import dev.zeldxh.relay.domain.Delivery;
import dev.zeldxh.relay.domain.Endpoint;
import dev.zeldxh.relay.domain.Event;
import dev.zeldxh.relay.domain.Organization;
import dev.zeldxh.relay.domain.Subscription;
import dev.zeldxh.relay.domain.Topic;
import dev.zeldxh.relay.repository.DeliveryRepository;
import dev.zeldxh.relay.repository.EndpointRepository;
import dev.zeldxh.relay.repository.EventRepository;
import dev.zeldxh.relay.repository.SubscriptionRepository;
import dev.zeldxh.relay.repository.TopicRepository;
import dev.zeldxh.relay.security.ApiKeyAuthFilter;
import dev.zeldxh.relay.streams.DeliveryStreamPublisher;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequestMapping("/topics")
public class TopicController {

    private final TopicRepository topicRepository;
    private final EndpointRepository endpointRepository;
    private final SubscriptionRepository subscriptionRepository;
    private final EventRepository eventRepository;
    private final DeliveryRepository deliveryRepository;
    private final DeliveryStreamPublisher deliveryStreamPublisher;
    private final AuditLogService auditLogService;

    public TopicController(TopicRepository topicRepository, EndpointRepository endpointRepository,
                            SubscriptionRepository subscriptionRepository, EventRepository eventRepository,
                            DeliveryRepository deliveryRepository, DeliveryStreamPublisher deliveryStreamPublisher,
                            AuditLogService auditLogService) {
        this.topicRepository = topicRepository;
        this.endpointRepository = endpointRepository;
        this.subscriptionRepository = subscriptionRepository;
        this.eventRepository = eventRepository;
        this.deliveryRepository = deliveryRepository;
        this.deliveryStreamPublisher = deliveryStreamPublisher;
        this.auditLogService = auditLogService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public TopicResponse create(@RequestAttribute(ApiKeyAuthFilter.ORGANIZATION_ATTRIBUTE) Organization organization,
                                 @Valid @RequestBody CreateTopicRequest request) {
        Topic topic = topicRepository.save(new Topic(organization, request.name()));
        auditLogService.record(organization, "topic.created", "name=" + request.name());
        return TopicResponse.from(topic);
    }

    @GetMapping
    public List<TopicResponse> list(@RequestAttribute(ApiKeyAuthFilter.ORGANIZATION_ATTRIBUTE) Organization organization) {
        return topicRepository.findByOrganization(organization).stream().map(TopicResponse::from).toList();
    }

    @GetMapping("/{topicId}/subscriptions")
    public List<SubscriptionResponse> listSubscriptions(@RequestAttribute(ApiKeyAuthFilter.ORGANIZATION_ATTRIBUTE) Organization organization,
                                                          @PathVariable Long topicId) {
        Topic topic = topicRepository.findByIdAndOrganization(topicId, organization)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "topic not found"));
        return subscriptionRepository.findByTopic(topic).stream().map(SubscriptionResponse::from).toList();
    }

    @PostMapping("/{topicId}/subscriptions")
    @ResponseStatus(HttpStatus.CREATED)
    @Transactional
    public SubscriptionResponse subscribe(@RequestAttribute(ApiKeyAuthFilter.ORGANIZATION_ATTRIBUTE) Organization organization,
                                           @PathVariable Long topicId,
                                           @Valid @RequestBody CreateSubscriptionRequest request) {
        Topic topic = topicRepository.findByIdAndOrganization(topicId, organization)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "topic not found"));
        Endpoint endpoint = endpointRepository.findByIdAndOrganization(request.endpointId(), organization)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "endpoint not found"));
        if (!endpoint.isVerified()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "endpoint must be verified before it can be subscribed to a topic");
        }

        Subscription subscription = subscriptionRepository.save(new Subscription(topic, endpoint));
        auditLogService.record(organization, "subscription.created", "topic=" + topic.getName() + ", endpointId=" + endpoint.getId());
        return SubscriptionResponse.from(subscription);
    }

    @PostMapping("/{topicId}/events")
    @ResponseStatus(HttpStatus.CREATED)
    @Transactional
    public EventResponse ingest(@RequestAttribute(ApiKeyAuthFilter.ORGANIZATION_ATTRIBUTE) Organization organization,
                                 @PathVariable Long topicId,
                                 @Valid @RequestBody IngestEventRequest request) {
        Topic topic = topicRepository.findByIdAndOrganization(topicId, organization)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "topic not found"));

        Event event = eventRepository.save(new Event(topic, request.payload().toString()));

        List<Subscription> subscriptions = subscriptionRepository.findByTopic(topic);
        List<Delivery> deliveries = subscriptions.stream()
                .map(subscription -> deliveryRepository.save(new Delivery(event, subscription.getEndpoint())))
                .toList();
        deliveries.forEach(delivery -> deliveryStreamPublisher.publish(delivery.getId()));

        return EventResponse.from(event, deliveries);
    }
}
