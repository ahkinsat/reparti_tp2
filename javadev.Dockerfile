# ---- Base image ----
FROM debian:stable

# ---- Environment ----
ENV DEBIAN_FRONTEND=noninteractive
ENV LANG=C.UTF-8

# ---- Install essential packages ----
RUN apt-get update && apt-get install --no-install-recommends -y  \
    curl \
    git \
    unzip \
    zip \
    sudo \
    ca-certificates \
    nano \
&& rm -rf /var/lib/apt/lists/*

# ---- Build arguments for user config ----
ARG USERNAME=dev
ARG USER_UID=1000
ARG USER_GID=1000

# ---- Create user and sudo access ----
RUN groupadd -g ${USER_GID} ${USERNAME} \
&& useradd -m -u ${USER_UID} -g ${USER_GID} -s /bin/bash ${USERNAME} \
&& echo "${USERNAME} ALL=(ALL) NOPASSWD:ALL" > /etc/sudoers.d/${USERNAME} \
&& chmod 0440 /etc/sudoers.d/${USERNAME}

# ---- SDKMAN setup for <USERNAME> only ----
ENV SDKMAN_DIR=/home/${USERNAME}/.sdkman
USER ${USERNAME}
WORKDIR /home/${USERNAME}

# Install SDKMAN as <USERNAME>
# Make SDKMAN available for <USERNAME> login shells
RUN curl -s "https://get.sdkman.io" | bash \
&& chmod -R 775 ${SDKMAN_DIR} \
&& echo "export SDKMAN_DIR=${SDKMAN_DIR}" >> /home/${USERNAME}/.bashrc \
&& echo "source ${SDKMAN_DIR}/bin/sdkman-init.sh" >> /home/${USERNAME}/.bashrc


# ---- Install default SDK tools ----
RUN bash -il -c 'sdk install java'
RUN bash -il -c 'sdk install maven'
# RUN bash -il -c 'sdk install gradle'
# RUN bash -il -c 'sdk install groovy'
RUN bash -il -c 'sdk flush archives && sdk flush temp'

# ---- Workspace ----
RUN mkdir /home/${USERNAME}/workspace
WORKDIR /home/${USERNAME}/workspace

# ---- Default shell ----    
CMD ["tail", "-f", "/dev/null"]
