alias net-down-rmq='podman network disconnect tp2_rabbitmq_network rabbitmq'
alias net-up-rmq='podman network connect tp2_rabbitmq_network rabbitmq'
alias net-down-ho='podman network disconnect tp2_ho_network ho_db'
alias net-up-ho='podman network connect tp2_ho_network ho_db'
alias net-down-bo1='podman network disconnect tp2_bo1_network bo1_db'
alias net-up-bo1='podman network connect tp2_bo1_network bo1_db'
alias net-down-bo2='podman network disconnect tp2_bo2_network bo2_db'
alias net-up-bo2='podman network connect tp2_bo2_network bo2_db'

alias java-shell='podman exec -it javadev bash -l'

