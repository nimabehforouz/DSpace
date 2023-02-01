/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.subscriptions;

import static org.dspace.core.Constants.COLLECTION;
import static org.dspace.core.Constants.COMMUNITY;
import static org.dspace.core.Constants.ITEM;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.dspace.content.DSpaceObject;
import org.dspace.core.Context;
import org.dspace.discovery.IndexableObject;
import org.dspace.eperson.Subscription;
import org.dspace.eperson.service.SubscribeService;
import org.dspace.scripts.DSpaceRunnable;
import org.dspace.scripts.handler.DSpaceRunnableHandler;
import org.dspace.subscriptions.service.DSpaceObjectUpdates;
import org.dspace.subscriptions.service.SubscriptionGenerator;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Implementation of {@link DSpaceRunnable} to find subscribed objects and send notification mails about them
 *
 * @author alba aliu
 */
public class SubscriptionEmailNotificationServiceImpl implements SubscriptionEmailNotificationService {

    private static final Logger log = LogManager.getLogger(SubscriptionEmailNotificationServiceImpl.class);

    /**
     * The map contains supported {SubscriptionParameter}
     * into the key configured {SubscriptionParameter.name}
     * instead into value configured a set of values {SubscriptionParameter.value}
     * related to {SubscriptionParameter.name}
     */
    private Map<String, Set<String>> param2values = new HashMap<>();
    private Map<String, DSpaceObjectUpdates> contentUpdates = new HashMap<>();
    @SuppressWarnings("rawtypes")
    private Map<String, SubscriptionGenerator> subscriptionType2generators = new HashMap<>();

    @Autowired
    private SubscribeService subscribeService;

    @SuppressWarnings("rawtypes")
    public SubscriptionEmailNotificationServiceImpl(Map<String, Set<String>> param2values,
                                                    Map<String, DSpaceObjectUpdates> contentUpdates,
                                                    Map<String, SubscriptionGenerator> subscriptionType2generators) {
        this.param2values = param2values;
        this.contentUpdates = contentUpdates;
        this.subscriptionType2generators = subscriptionType2generators;
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    public void perform(Context context, DSpaceRunnableHandler handler, String subscriptionType, String frequency) {
        List<IndexableObject> items = new ArrayList<>();
        List<IndexableObject> communities = new ArrayList<>();
        List<IndexableObject> collections = new ArrayList<>();
        try {
            List<Subscription> subscriptions =
                               findAllSubscriptionsBySubscriptionTypeAndFrequency(context, subscriptionType, frequency);
            // Here is verified if SubscriptionType is "content" Or "statistics" as them are configured
            if (subscriptionType2generators.keySet().contains(subscriptionType)) {
                // the list of the person who has subscribed
                int iterator = 0;
                for (Subscription subscription : subscriptions) {
                    DSpaceObject dSpaceObject = subscription.getDSpaceObject();

                    if (dSpaceObject.getType() == COMMUNITY) {
                        communities.addAll(contentUpdates.get("community")
                                                         .findUpdates(context, dSpaceObject, frequency));
                    } else if (dSpaceObject.getType() == COLLECTION) {
                        collections.addAll(contentUpdates.get("collection")
                                                         .findUpdates(context, dSpaceObject, frequency));
                    } else if (dSpaceObject.getType() == ITEM) {
                        items.addAll(contentUpdates.get("item").findUpdates(context, dSpaceObject, frequency));
                    }

                    var ePerson = subscription.getEPerson();
                    if (iterator < subscriptions.size() - 1) {
                        // as the subscriptions are ordered by eperson id, so we send them by ePerson
                        if (ePerson.equals(subscriptions.get(iterator + 1).getEPerson())) {
                            iterator++;
                            continue;
                        } else {
                            subscriptionType2generators.get(subscriptionType)
                                      .notifyForSubscriptions(context, ePerson, communities, collections, items);
                            communities.clear();
                            collections.clear();
                            items.clear();
                        }
                    } else {
                        //in the end of the iteration
                        subscriptionType2generators.get(subscriptionType)
                                        .notifyForSubscriptions(context, ePerson, communities, collections, items);
                    }
                    iterator++;
                }
            } else {
                throw new IllegalArgumentException("Currently this SubscriptionType:" + subscriptionType +
                                                   " is not supported!");
            }
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            handler.handleException(e);
            context.abort();
        }
    }

    /**
     * Return all Subscriptions by subscriptionType and frequency ordered by ePerson ID
     * if there are none it returns an empty list
     * 
     * @param context            DSpace context
     * @param subscriptionType   Could be "content" or "statistics". NOTE: in DSpace we have only "content"
     * @param frequency          Could be "D" stand for Day, "W" stand for Week, and "M" stand for Month
     * @return
     */
    private List<Subscription> findAllSubscriptionsBySubscriptionTypeAndFrequency(Context context,
             String subscriptionType, String frequency) {
        try {
            return subscribeService.findAllSubscriptionsBySubscriptionTypeAndFrequency(context, subscriptionType,
                                    frequency)
                                   .stream()
                                   .sorted(Comparator.comparing(s -> s.getEPerson().getID()))
                                   .collect(Collectors.toList());
        } catch (SQLException e) {
            log.error(e.getMessage(), e);
        }
        return new ArrayList<Subscription>();
    }

    @Override
    public Set<String> getSupportedSubscriptionTypes() {
        return subscriptionType2generators.keySet();
    }

    @Override
    public Set<String> getSubscriptionParameterValuesByName(String name) {
        return param2values.containsKey(name) ? param2values.get(name) : Collections.emptySet();
    }

    @Override
    public boolean isSupportedSubscriptionParameterName(String name) {
        return param2values.containsKey(name);
    }

}
