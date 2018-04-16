package com.my.p2p;

import android.app.Application;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class P2PApplication extends Application {

    static final String ACTION_START_SERVICE = "com.my.p2p.START_SERVICE";

    boolean isOnline = false;

    final Set<String> reqs = new HashSet<>();
    final List<Map<String, Object>> users = new ArrayList<>();
    final List<Map<String, Object>> requests = new ArrayList<>();
    final List<Map<String, Object>> connects = new ArrayList<>();
}
