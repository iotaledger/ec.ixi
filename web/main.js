let $input_seed;
let $input_merkle_tree_depth;
let $input_cluster_address;
let $input_cluster_trust;

let $input_transfers_seed;
let $input_transfers_index;
let $input_transfers_value;
let $input_transfers_receiver;
let $input_transfers_remainder;

let $wallet_table;
let $cluster_table;
let $actors_table;
let $transfers_table;

window.onload = function () {
    init_elements();
    refresh_wallet();
    refresh_actors();
    refresh_cluster();
    refresh_transfers();
};

/**
 * Initializes global jQuery pointers to commonly used DOM objects.
 * */
function init_elements() {
    $input_seed = $('#seed');
    $input_merkle_tree_depth = $('#merkle_tree_depth');
    $input_cluster_address = $('#cluster_address');
    $input_cluster_trust = $('#cluster_trust');

    $input_transfers_seed = $('#transfers_seed');
    $input_transfers_index = $('#transfers_index');
    $input_transfers_value = $('#transfers_value');
    $input_transfers_receiver = $('#transfers_receiver');
    $input_transfers_remainder = $('#transfers_remainder');

    $input_seed.val(random_trytes(81));
    $input_merkle_tree_depth.val(3);
    $input_cluster_trust.val(0.2);

    $wallet_table = $('#wallet table');
    $cluster_table = $('#cluster table');
    $actors_table = $('#actors table');
    $transfers_table = $('#transfers table');
}

function submit_transfer_button() {
    const seed = $input_transfers_seed.val();
    const index = parseInt($input_transfers_index.val());
    const receiver = $input_transfers_receiver.val();
    const remainder = $input_transfers_remainder.val();
    const value = $input_transfers_value.val();
    submit_transfer(seed, index, receiver, remainder, value, alert);
}

function create_actor_button() {
    const merkle_tree_depth = parseInt($input_merkle_tree_depth.val());
    const seed = random_trytes(81);
    create_actor(seed, merkle_tree_depth, 0);
}

function add_actor_button() {
    const address = $input_cluster_address.val();
    const trust = parseFloat($input_cluster_trust.val());
    add_actor(address, trust);
}

function random_trytes(length) {
    let seed = "";
    for(let i = 0; i < length; i++)
        seed += random_tryte();
    return seed;
}

function random_tryte() {
    const TRYTES = "ABCDEFGHIJKLMNOPQRSTUVWXYZ9";
    return TRYTES.charAt(Math.floor(Math.random()*27));
}

/**
 * Loads and displays the addresses with their respective balances in the wallet section.
 * */
function refresh_wallet() {
    get_balances($input_seed.val(), display_balances);
}

function refresh_actors() {
    get_actors(display_actors);
}

function refresh_cluster() {
    get_cluster(display_cluster)
}

function refresh_transfers() {
    get_transfers(display_transfers)
}

function display_balances(balances) {
    $wallet_table.html(gen_table_row(["address", "balance"], true));
    $.each(balances, function (index, entry) {
        $wallet_table.append(gen_table_row([entry['address'], entry['balance']]));
    });
}

function display_actors(actors) {
    $actors_table.html(gen_table_row(["address"], true));
    $.each(actors, function (index, actor) {
        $actors_table.append(gen_table_row([actor]));
    });
}

function display_cluster(actors) {
    $cluster_table.html(gen_table_row(["address", "trust"], true));
    $.each(actors, function (index, entry) {
        $cluster_table.append(gen_table_row([entry["address"], entry["trust"]]));
    });
}

function display_transfers(transfers) {
    $transfers_table.html(gen_table_row(["hash of head"], true));
    $.each(transfers, function (index, hash) {
        $transfers_table.append(gen_table_row([hash]));
    });
}

/**
 * @param cells array of row's cell contents in correct order
 * @param th if enabled will use <th> cells instead of <td>
 * @return table row whose cells are filled with respective content
 * */
function gen_table_row(cells, th = false) {
    const $tr = $("<tr>");
    $.each(cells, function (_, cell) {
        $tr.append($(th ? "<th>" : "<td>").text(cell));
    });
    return $tr;
}

function submit_transfer(seed, index, receiver, remainder, value, callback) {
    const request ={
        "action": "submit_transfer",
        "seed": seed,
        "index": index,
        "receiver": receiver,
        "remainder": remainder,
        "value": value
    };
    ec_request(request, response => callback(response['hash']));
}

function get_transfers(callback) {
    ec_request({"action": "get_transfers"}, response => callback(response['transfers']));
}

/**
 * @param seed {string} The seed to derive the addresses from.
 * @param {get_balances_callback} callback
 * */
function get_balances(seed, callback) {
    /**
     * @callback get_balances_callback
     * @param {json} balances map of the addresses derived from this seed with their respective balances in iotas
     */
    ec_request({"action": "get_balances", "seed": seed}, response => callback(response['balances']));
}

/**
 * @param {get_actors_callback} callback
 * */
function get_actors(callback) {
    /**
     * @callback get_actors_callback
     * @param {array} addresses of all actors
     */
    ec_request({"action": "get_actors"}, response => callback(response['actors']));
}

/**
 * @param {get_cluster_callback} callback
 * */
function get_cluster(callback) {
    /**
     * @callback get_cluster_callback
     * @param {array} list of actors (json object with string field 'address' and double field 'trust')
     */
    ec_request({"action": "get_cluster"}, response => callback(response['cluster']));
}

function create_actor(seed, merkle_tree_depth, start_index) {
    ec_request({"action": "create_actor", "seed": seed, "merkle_tree_depth": merkle_tree_depth, "start_index": start_index}, refresh_actors);
}

function add_actor(address, trust) {
    ec_request({"action": "add_actor", "address": address, "trust": trust}, refresh_cluster);
}

function ec_request(request, success) {
    ajax("getModuleResponse", {"request": JSON.stringify(request), "path": "ec.ixi-1.0.jar"}, data => {
        const response = JSON.parse(data['response']);
        response['success'] ? (success ? success(response) : {}) : console.error("api error: " + response['error']);
    });
}

function ajax(path, data, success) {
    data['password'] = "change_me_now";
    $.ajax({
        url: "http://localhost:2187/" + path,
        method: "POST",
        data: serialize_post_data(data),
        dataType: "json",
        success: success,
        error: function (error) {
            alert(JSON.stringify(error));
        }
    });
}

function serialize_post_data(data) {
    const serialized = [];
    $.each(data, function (name, value) {
        serialized.push({"name": name, "value": value});
    });
    return serialized;
}