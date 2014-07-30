package controllers;

import models.NewNotification;

import java.util.List;

/**
 * This class contains some static helper methods for templates.
 */
public class TemplateHelper {
    /**
     * Returns the number of unread notifications.
     *
     * @param notifications List of notifications
     * @return Count of unread notifications
     */
    public static int countUnreadNotifications(List<NewNotification> notifications) {
        int count = 0;

        for (NewNotification notification : notifications) {
            if (!notification.isRead) {
                count++;
            }
        }

        return count;
    }
}
