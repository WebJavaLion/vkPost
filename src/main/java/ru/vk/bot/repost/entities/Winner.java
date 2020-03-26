package ru.vk.bot.repost.entities;


import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import javax.persistence.*;

@Entity
@Table(name = "established_winners")
@Data
public class Winner {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Integer id;

    Integer telegramId;

    String userName;

    String firstName;

    String lastName;

    @ManyToOne
    @JoinColumn(
            name = "competition_id",
            referencedColumnName = "id")
    Competition competition;

}

