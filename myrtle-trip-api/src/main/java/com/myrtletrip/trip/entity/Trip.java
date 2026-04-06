package com.myrtletrip.trip.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "trip")
public class Trip {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false)
    private String name;

    @Column(name = "trip_year", nullable = false)
    private Integer tripYear;

    // NEW: used for handicap grouping
    @Column(name = "trip_code", nullable = false, unique = true)
    private String tripCode;

    // NEW: prevents re-initializing accidentally later
    @Column(name = "initialized", nullable = false)
    private Boolean initialized = false;

    // getters/setters...

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public Integer getTripYear() {
		return tripYear;
	}

	public void setTripYear(Integer tripYear) {
		this.tripYear = tripYear;
	}

	public String getTripCode() {
		return tripCode;
	}

	public void setTripCode(String tripCode) {
		this.tripCode = tripCode;
	}

	public Boolean getInitialized() {
		return initialized;
	}

	public void setInitialized(Boolean initialized) {
		this.initialized = initialized;
	}

	public Long getId() {
		return id;
	}

 }