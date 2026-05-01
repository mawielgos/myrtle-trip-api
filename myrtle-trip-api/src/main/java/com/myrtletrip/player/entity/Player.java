package com.myrtletrip.player.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "player")
public class Player {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;
	
    @Column(name = "first_name")
    private String firstName;

    @Column(name = "last_name")
    private String lastName;

    @Column(name = "display_name")
    private String displayName;

    @Column(name = "ghin_number", length = 25)
    private String ghinNumber;

    private boolean active;
    
    @Column(name = "email")
    private String email;

    @Column(name = "cell")
    private String cell;

    @Column(name = "venmo_id")
    private String venmoId;

    @Column(name = "zelle_id")
    private String zelleId;

    @Column(name = "legacy_player_number")
    private Integer legacyPlayerNumber;
 
    @Column(name = "handicap_method", length = 30)
    private String handicapMethod;

    @Column(name = "gender", length = 1, nullable = false)
    private String gender = "M";

    public String getHandicapMethod() {
        return handicapMethod;
    }

    public void setHandicapMethod(String handicapMethod) {
        this.handicapMethod = handicapMethod;
    }
    // getters/setters (or Lombok if enabled)
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getFirstName() { return firstName; }
    public void setFirstName(String firstName) { this.firstName = firstName; }

    public String getLastName() { return lastName; }
    public void setLastName(String lastName) { this.lastName = lastName; }

    public String getDisplayName() { return displayName; }
    public void setDisplayName(String displayName) { this.displayName = displayName; }

    public String getGhinNumber() { return ghinNumber; }
    public void setGhinNumber(String ghinNumber) { this.ghinNumber = ghinNumber; }

    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
	public String getEmail() {
		return email;
	}
	public void setEmail(String email) {
		this.email = email;
	}
	public String getCell() {
		return cell;
	}
	public void setCell(String cell) {
		this.cell = cell;
	}
	public String getVenmoId() {
		return venmoId;
	}
	public void setVenmoId(String venmoId) {
		this.venmoId = venmoId;
	}
	public String getZelleId() {
		return zelleId;
	}
	public void setZelleId(String zelleId) {
		this.zelleId = zelleId;
	}
	public Integer getLegacyPlayerNumber() {
		return legacyPlayerNumber;
	}
	public void setLegacyPlayerNumber(Integer legacyPlayerNumber) {
		this.legacyPlayerNumber = legacyPlayerNumber;
	}

    public String getGender() {
        return gender;
    }

    public void setGender(String gender) {
        this.gender = gender;
    }
}
