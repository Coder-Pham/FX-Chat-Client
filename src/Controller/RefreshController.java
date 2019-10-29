package Controller;

import Model.User;

import java.util.ArrayList;

public final class RefreshController {
    private static Message controller;

    public static void refreshUOL(ArrayList<User> UOL) {
        controller.refreshUserList(UOL);
    }

    public static void setController(Message controller) {
        RefreshController.controller = controller;
    }
}
