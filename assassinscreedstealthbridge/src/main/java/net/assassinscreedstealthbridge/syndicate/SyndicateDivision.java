package net.assassinscreedstealthbridge.syndicate;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class SyndicateDivision {

    public static final int MIN_MEMBERS = 2;
    public static final int MAX_MEMBERS = 5;

    public final String id;
    public String name;
    public String rewardFlavor;
    public UUID leaderId;
    public final List<UUID> memberIds = new ArrayList<>();
    
    // NEU: Intel-Fortschrittsbalken (0 bis 100)
    public int intel = 0; 

    public SyndicateDivision(String id, String name, String rewardFlavor) {
        this.id = id;
        this.name = name;
        this.rewardFlavor = rewardFlavor;
    }
}