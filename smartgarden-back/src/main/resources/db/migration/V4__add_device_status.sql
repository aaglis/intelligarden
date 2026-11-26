-- Estado de conexão informado pelo próprio dispositivo via MQTT (tópico .../status, com last will).
alter table devices
    add column if not exists online boolean not null default false;

alter table devices
    add column if not exists status_changed_at timestamp with time zone;

alter table devices
    add column if not exists ip_address varchar(45);

alter table devices
    add column if not exists firmware_version varchar(30);
