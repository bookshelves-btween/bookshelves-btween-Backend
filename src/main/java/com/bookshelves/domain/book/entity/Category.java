package com.bookshelves.domain.book.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Category {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "kdc_code", unique = true, length = 3)
  private String kdcCode;

  @Column(name = "name", nullable = false, length = 50)
  private String name;

  private Category(String kdcCode, String name) {
    this.kdcCode = kdcCode;
    this.name = name;
  }

  public static Category create(String kdcCode, String name) {
    return new Category(kdcCode, name);
  }

  public void updateMasterData(String kdcCode, String name) {
    this.kdcCode = kdcCode;
    this.name = name;
  }
}
