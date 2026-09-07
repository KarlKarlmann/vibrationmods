package net.assassinscreedstealthbridge.syndicate;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class SyndicateMember {

    public static final int RANK_UNASSIGNED = 0;
    public static final int RANK_SERGEANT = 1;
    public static final int RANK_LIEUTENANT = 2;
    public static final int RANK_CAPTAIN = 3;

    public final UUID id;
    public String name;
    public String divisionId;
    public int rank;
    public boolean leader;
    public UUID capturedBy = null; 
    
    // NEU: Der Signature Buff für diesen Charakter
    public String signatureEffect;

    public final Set<UUID> trusted = new HashSet<>();
    public final Set<UUID> rivals = new HashSet<>();

    public SyndicateMember(UUID id, String name, String divisionId, int rank, boolean leader, String signatureEffect) {
        this.id = id;
        this.name = name;
        this.divisionId = divisionId;
        this.rank = rank;
        this.leader = leader;
        this.signatureEffect = signatureEffect;
    }

    public boolean isAssigned() {
        return divisionId != null && !divisionId.isEmpty();
    }

    public String getRankTitle() {
        return switch (rank) {
            case RANK_SERGEANT -> "Sergeant";
            case RANK_LIEUTENANT -> "Lieutenant";
            case RANK_CAPTAIN -> "Captain";
            default -> "Recruit";
        };
    }

    public String getRankStars() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < RANK_CAPTAIN; i++) {
            sb.append(i < rank ? "\u2605" : "\u2606"); 
        }
        return sb.toString();
    }
}