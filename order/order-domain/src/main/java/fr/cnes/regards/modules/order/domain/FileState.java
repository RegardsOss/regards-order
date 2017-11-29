package fr.cnes.regards.modules.order.domain;

/**
 * In case a file is nearline, it is at one of these states :
 * - PENDING : called to storage to make it available,
 * - AVAILABLE : available to be downloaded,
 * - DOWNLOADED : already downloaded (maybe no more available),
 * - ERROR : in error while asked to be made available.
 * Else, status is :
 * - ONLINE : not stored into rs-storage BUT managed by rs-storage as immediately available
 * @author oroussel
 */
public enum FileState {
    AVAILABLE,
    DOWNLOADED,
    ERROR,
    PENDING,

    ONLINE
}
