package com.workflow.auth.entity;

import java.sql.Time;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@EqualsAndHashCode
@ToString
@Entity(name="users")
@Table(name="users")
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Builder
public class AuthEntity {
	
	@Id
	private int id;
	
	private String email;
	private String passwordHash;
	private String name;
	private String department;
	private String position;
	private String role;
	private String status;
	private Time lastLoginAt;
	private Time createdAt;
	private Time updatedAt;

}